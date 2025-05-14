package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._
import cpu.CpuConfig

class CsrRead extends Bundle {
  val ren    = Output(Bool())
  val raddr  = Output(UInt(VT_CSR_ADDR_WID.W))
  val rdata  = Input(UInt(XLEN.W))
  val rlegal = Input(Bool())
}

class CsrWrite extends Bundle {
  val wen    = Output(Bool())
  val waddr  = Output(UInt(VT_CSR_ADDR_WID.W))
  val wdata  = Output(UInt(XLEN.W))
  val wlegal = Input(Bool())
}

class CSRRegFile extends Module {
  val io = IO(new Bundle {
    // with CSR (in FU)
    val read      = Flipped(new CsrRead())
    val write     = Flipped(new CsrWrite())

    // mode -> DecodeUnit
    val mode      = Output(UInt(MODE_WID.W))

    // External Interruptions
    val ext_int   = Input(new ExtInterrupt())

    // Handle/Return Exceptions & Interuptions
    val ex        = Input(new ExceptionInfo())
    val mret      = Input(Bool())
    val pc        = Input(UInt(XLEN.W))
    val ftcInfo   = Output(new FetchInfo())
    val interrupt = Output(Vec(INT_WID, Bool()))
  })

  // Address Map
  val empty      =  0
  val cycle      =  1
  val mvendorid  =  2
  val marchid    =  3
  val mimpid     =  4
  val mhartid    =  5
  val mstatus    =  6
  val misa       =  7
  val mie        =  8
  val mtvec      =  9
  val mcounteren = 10
  val mscratch   = 11
  val mepc       = 12
  val mcause     = 13
  val mtval      = 14
  val mip        = 15
  val satp       = 16
  val medeleg    = 17
  val mideleg    = 18
  val pmpcfg0    = 19
  val pmpaddr0   = 20
  val tselect    = 21
  val tdata1     = 22
  val tdata2     = 23

  // vt_addr -> addr, rmask, wmask
  val addr_default = List(empty.U, "h00000000_00000000".U, "h00000000_00000000".U)

  val addr_table = Array(
    BitPat(CSRAddr.cycle)      -> List(cycle.U,      "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    
    BitPat(CSRAddr.mvendorid)  -> List(mvendorid.U,  "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    BitPat(CSRAddr.marchid)    -> List(marchid.U,    "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    BitPat(CSRAddr.mimpid)     -> List(mimpid.U,     "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    BitPat(CSRAddr.mhartid)    -> List(mhartid.U,    "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),

    BitPat(CSRAddr.mstatus)    -> List(mstatus.U,    "hFFFFFFFF_FFFFFFFF".U, "h00000000_00021888".U),
    // BitPat(CSRAddr.medeleg)    -> List(medeleg.U,    "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    // BitPat(CSRAddr.mideleg)    -> List(mideleg.U,    "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.misa)       -> List(misa.U,       "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    BitPat(CSRAddr.mie)        -> List(mie.U,        "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.mtvec)      -> List(mtvec.U,      "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.mcounteren) -> List(mcounteren.U, "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    
    BitPat(CSRAddr.mscratch)   -> List(mscratch.U,   "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.mepc)       -> List(mepc.U,       "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.mcause)     -> List(mcause.U,     "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.mtval)      -> List(mtval.U,      "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.mip)        -> List(mip.U,        "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000888".U),
    
    BitPat(CSRAddr.satp)       -> List(satp.U,       "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.pmpcfg0)    -> List(pmpcfg0.U,    "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.pmpaddr0)   -> List(pmpaddr0.U,   "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    
    BitPat(CSRAddr.tselect)    -> List(tselect.U,    "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFE".U),
    BitPat(CSRAddr.tdata1)     -> List(tdata1.U,     "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.tdata2)     -> List(tdata2.U,     "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
  )

  // Initialize Registers
  val reg_mode = RegInit(Privilege.m)
  val regs = RegInit(VecInit(
    Seq.tabulate(CREG_NUM) {
      i =>
      i match {
        case `mstatus` => "h00000002_00000000".U(XLEN.W)
        case `misa`    => "h80000000_00101100".U(XLEN.W)
        case `tselect` => "h00000000_00000001".U(XLEN.W)
        case _         => 0.U(XLEN.W)
      }
    }
  ))

  // Read (with CSR)
  io.read.rdata  := 0.U
  io.read.rlegal := true.B

  when (io.read.ren) {
    val vt_addr = io.read.raddr
    val addr :: mask :: nop :: Nil =
      ListLookup(io.read.raddr, addr_default, addr_table)
    when (addr === empty.U || !CSRAddr.enRead(vt_addr) || reg_mode < CSRAddr.leastMode(vt_addr)) {
      io.read.rlegal := false.B
    }.otherwise {
      io.read.rdata := regs(addr) & mask
    }
  }

  // Write (with CSR)
  io.write.wlegal := true.B

  when (io.write.wen) {
    val vt_addr = io.write.waddr
    val addr :: nop :: mask :: Nil =
      ListLookup(io.write.waddr, addr_default, addr_table)
    when (addr === empty.U || !CSRAddr.enWrite(vt_addr) || reg_mode < CSRAddr.leastMode(vt_addr)) {
      io.write.wlegal := false.B
    }.otherwise {
      // sxl : 35, 34
      // uxl : 33, 32
      // mpp : 12, 11
      val wdata = io.write.wdata
      val sxl   = regs(mstatus)(35, 34)
      val uxl   = regs(mstatus)(33, 32)
      val wmpp  = Mux(
        (sxl === 0.U && wdata(12, 11) === Privilege.s) ||
        (uxl === 0.U && wdata(12, 11) === Privilege.u) ||
        (wdata(12, 11) === Privilege.h),
        regs(mstatus)(12, 11),
        wdata(12, 11)
      )
      regs(addr) :=
        (wdata & mask & "hFFFFFFFF_FFFFE7FF".U) |
        wmpp << 11 |
        (regs(addr) & ~mask)
    }
  }

  // mode -> DecodeUnit
  io.mode := reg_mode

  // External Interruptions
  val ex = WireInit(io.ex)

  when (io.ext_int.mei) { ex.interrupt(macExternalInterrupt) := true.B }
  when (io.ext_int.mti) { ex.interrupt(macTimerInterrupt) := true.B }
  when (io.ext_int.msi) { ex.interrupt(macSoftwareInterrupt) := true.B }

  // Handle Exceptions & Interuptions
  // Find the First Interrupt/Exception
  val excCases = Exceptionity.zipWithIndex.map { 
    case (value, index) =>
      ex.exception(value) -> value.U
  }

  def processInterrupt(int_id : Int) = {
    val case0 = ex.interrupt(int_id)
    val case1 = ((reg_mode === Privilege.m) && (regs(mstatus)(3) === 1.U)) || (reg_mode < Privilege.m)
    val case2 = (regs(mip)(int_id) === 1.U) && (regs(mie)(int_id) === 1.U)
    case0 && case1 && case2
  }
  
  val intCases = Interruptionity.zipWithIndex.map {
    case (value, index) =>
      processInterrupt(value) -> value.U
  }

  val excNO = MuxCase(noException.U, excCases)
  val intNO = MuxCase(noInterrupt.U, intCases)
  val isInt = intNO =/= noInterrupt.U
  val cauNO = Mux(isInt, intNO, excNO).pad(XLEN)

  // Handle the First Interrupt/Exception
  val flush  = WireInit(false.B)
  val target = WireInit(0.U(XLEN.W))

  when (intNO =/= noInterrupt.U || excNO =/= noException.U) {
    regs(mtval)   := Mux(isInt, 0.U(XLEN.W), ex.tval(excNO))
    regs(mcause)  := isInt << 63 | cauNO
    regs(mepc)    := io.pc
    reg_mode      := Privilege.m
    flush         := true.B

    // mpp  : 12, 11
    // mpie : 7
    // mie  : 3
    // mpie <- mie
    // mie  <- 0
    // mpp  <- mode
    regs(mstatus) := (
      regs(mstatus) & ~(
        (1.U <<  3) |
        (1.U <<  7) |
        (3.U << 11)
      ).pad(XLEN)) |
      (regs(mstatus)(3) <<  7) |
      (reg_mode         << 11)

    target        := (
      (regs(mtvec)(63, 2) << 2) +
      Mux(
        regs(mtvec)(0) && isInt,
        (cauNO << 2)(XLEN - 1, 0),
        0.U(XLEN.W)
      )
    )
  }

  // mret : Return Exceptions & Interuptions
  when (io.mret) {
    reg_mode      := regs(mstatus)(12, 11)
    flush         := true.B
    target        := regs(mepc)
    
    // mprv : 17
    // mpp  : 12, 11
    // mpie : 7
    // mie  : 3
    // mprv <- mpp === M ? mprv : 0
    // mpp  <- U
    // mie  <- mpie
    // mpie <- 1
    val mprv_new = Mux(
      regs(mstatus)(12, 11) === Privilege.m,
      regs(mstatus)(17),
      "b0".U
    )

    regs(mstatus) := (
      regs(mstatus) & ~(
        (1.U <<  3) |
        (1.U <<  7) |
        (3.U << 11) |
        (1.U << 17)
      ).pad(XLEN)) |
      (regs(mstatus)(7) <<  3) |
      (1.U              <<  7) |
      (Privilege.u      << 11) |
      (mprv_new         << 17)
  }

  io.ftcInfo.flush  := flush
  io.ftcInfo.target := target

  // interrupt -> DecodeUnit
  io.interrupt := VecInit(Seq.fill(INT_WID)(false.B))

  when (intNO =/= noInterrupt.U) {
    io.interrupt(intNO) := true.B
  }
}
