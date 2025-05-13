// LAB8: CRegFile : Registers of CSR

package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._
import cpu.CpuConfig

class CsrRead extends Bundle {
  val raddr  = Output(UInt(VT_CSR_ADDR_WID.W))
  val rdata  = Input(UInt(XLEN.W))

  // LAB9: CsrRead : ren & rlegal
  val ren    = Output(Bool())
  val rlegal = Input(Bool())
}

class CsrWrite extends Bundle {
  val wen    = Output(Bool())
  val waddr  = Output(UInt(VT_CSR_ADDR_WID.W))
  val wdata  = Output(UInt(XLEN.W))

  // LAB9: CsrWrite : wlegal
  val wlegal = Input(Bool())
}

class CSRRegFile extends Module {
  val io = IO(new Bundle {
    val read      = Flipped(new CsrRead())
    val write     = Flipped(new CsrWrite())

    // LAB9: New Input : executeUnit -> ExceptionInfo, mret, pc
    val ex        = Input(new ExceptionInfo())
    val mret      = Input(Bool())
    val pc        = Input(UInt(XLEN.W))
    // LAB9: New Input : Core -> ExtInterrupt
    val ext_int   = Input(new ExtInterrupt())
    // LAB9: New Output : ftcInfo -> ExceptionInfo
    val ftcInfo   = Output(new FetchInfo())
    // LAB9: New Output : mode & interrupt -> decodeUnit
    val mode      = Output(UInt(MODE_WID.W))
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

  // LAB9: new CSRs
  val satp       = 16
  val medeleg    = 17
  val mideleg    = 18
  val pmpcfg0    = 19
  val pmpaddr0   = 20
  val tselect    = 21
  val tdata1     = 22
  val tdata2     = 23

  val addr_default = List(empty.U, "h00000000_00000000".U, "h00000000_00000000".U)
  
  val addr_table = Array(
    // BitPat("b1100_0000_0000") -> List(cycle,      "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    // BitPat("b1111_0001_0001") -> List(mvendorid,  "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    // BitPat("b1111_0001_0010") -> List(marchid,    "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    // BitPat("b1111_0001_0011") -> List(mimpid,     "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    // BitPat("b1111_0001_0101") -> List(mhartid,    "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    // // BitPat("b0011_0000_0000") -> List(mstatus,    "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000088".U),
    // BitPat("b0011_0000_0001") -> List(misa,       "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    // BitPat("b0011_0000_0100") -> List(mie,        "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    // BitPat("b0011_0000_0100") -> List(mtvec,      "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    // BitPat("b0011_0000_0110") -> List(mcounteren, "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    // BitPat("b0011_0100_0000") -> List(mscratch,   "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    // BitPat("b0011_0100_0001") -> List(mepc,       "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    // BitPat("b0011_0100_0010") -> List(mcause,     "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    // BitPat("b0011_0100_0011") -> List(mtval,      "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    // BitPat("b0011_0100_0100") -> List(mip,        "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000888".U),

    // // LAB9: Update the Writing Mask of mstatus
    // BitPat("b0011_0000_0000") -> List(mstatus,    "hFFFFFFFF_FFFFFFFF".U, "h00000000_00021888".U),

    // LAB9: csr addr_table : vt_addr -> addr, rmask, wmask
    BitPat(CSRAddr.cycle_mask)      -> List(cycle.U,      "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    BitPat(CSRAddr.mvendorid_mask)  -> List(mvendorid.U,  "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    BitPat(CSRAddr.marchid_mask)    -> List(marchid.U,    "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    BitPat(CSRAddr.mimpid_mask)     -> List(mimpid.U,     "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    BitPat(CSRAddr.mhartid_mask)    -> List(mhartid.U,    "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    BitPat(CSRAddr.mstatus_mask)    -> List(mstatus.U,    "hFFFFFFFF_FFFFFFFF".U, "h00000000_00021888".U),
    BitPat(CSRAddr.misa_mask)       -> List(misa.U,       "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    BitPat(CSRAddr.mie_mask)        -> List(mie.U,        "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.mtvec_mask)      -> List(mtvec.U,      "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.mcounteren_mask) -> List(mcounteren.U, "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.mscratch_mask)   -> List(mscratch.U,   "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.mepc_mask)       -> List(mepc.U,       "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.mcause_mask)     -> List(mcause.U,     "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.mtval_mask)      -> List(mtval.U,      "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.mip_mask)        -> List(mip.U,        "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000888".U),
    
    BitPat(CSRAddr.satp_mask)       -> List(satp.U,       "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    // BitPat(CSRAddr.medeleg_mask)    -> List(medeleg.U,    "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    // BitPat(CSRAddr.mideleg_mask)    -> List(mideleg.U,    "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.pmpcfg0_mask)    -> List(pmpcfg0.U,    "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.pmpaddr0_mask)   -> List(pmpaddr0.U,   "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.tselect_mask)    -> List(tselect.U,    "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFE".U),
    BitPat(CSRAddr.tdata1_mask)     -> List(tdata1.U,     "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat(CSRAddr.tdata2_mask)     -> List(tdata2.U,     "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
  )

  // Initialize Registers
  // val regs = RegInit(VecInit(Seq.fill(CREG_NUM)(0.U(XLEN.W))))
  
  // LAB9: Initialize mode
  val reg_mode = RegInit(Privilege.m)

  // regs(mstatus) := "h00000000_00001800".U
  // regs(misa)    := "h80000000_00001100".U

  // LAB9: Update the Initial Value of mstatus & misa
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

  // Read
  // val raddr :: rmask :: r_nop :: Nil =
  //   ListLookup(io.read.raddr, addr_default, addr_table)

  // io.read.rdata := regs(raddr) & rmask

  // Write
  // val waddr :: w_nop :: wmask :: Nil =
  //   ListLookup(io.write.waddr, addr_default, addr_table)

  // when (io.write.wen && waddr =/= 0.U) {
  //   regs(waddr) := (io.write.wdata & wmask) | (regs(waddr) & ~wmask)
  // }

  // LAB9: Read & Write
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

  // LAB9: Read mode
  io.mode := reg_mode

  // LAB9: ext_int -> ex
  val ex = Wire(new ExceptionInfo())
  
  ex := io.ex

  when (io.ext_int.mei) { ex.interrupt(macExternalInterrupt) := true.B }
  when (io.ext_int.mti) { ex.interrupt(macTimerInterrupt) := true.B }
  when (io.ext_int.msi) { ex.interrupt(macSoftwareInterrupt) := true.B }

  // LAB9: Exceptions

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
  val one   = 1.U(XLEN.W)

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

  io.interrupt := VecInit(Seq.fill(INT_WID)(false.B))

  when (intNO =/= noInterrupt.U) {
    io.interrupt(intNO) := true.B
  }

  // LAB9: mret
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

}
