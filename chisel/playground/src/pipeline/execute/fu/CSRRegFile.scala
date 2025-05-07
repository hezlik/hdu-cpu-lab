// LAB8: CRegFile : Registers of CSR

package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._
import cpu.CpuConfig

class CsrRead extends Bundle {
  val raddr = Output(UInt(VT_CSR_ADDR_WID.W))
  val rdata = Input(UInt(XLEN.W))
}

class CsrWrite extends Bundle {
  val wen   = Output(Bool())
  val waddr = Output(UInt(VT_CSR_ADDR_WID.W))
  val wdata = Output(UInt(XLEN.W))
}

class CSRRegFile extends Module {
  val io = IO(new Bundle {
    val read  = Flipped(new CsrRead())
    val write = Flipped(new CsrWrite())
  })

  // Address Map
  val empty      =  0.U(CSR_ADDR_WID.W)
  val cycle      =  1.U(CSR_ADDR_WID.W)
  val mvendorid  =  2.U(CSR_ADDR_WID.W)
  val marchid    =  3.U(CSR_ADDR_WID.W)
  val mimpid     =  4.U(CSR_ADDR_WID.W)
  val mhartid    =  5.U(CSR_ADDR_WID.W)
  val mstatus    =  6.U(CSR_ADDR_WID.W)
  val misa       =  7.U(CSR_ADDR_WID.W)
  val mie        =  8.U(CSR_ADDR_WID.W)
  val mtvec      =  9.U(CSR_ADDR_WID.W)
  val mcounteren = 10.U(CSR_ADDR_WID.W)
  val mscratch   = 11.U(CSR_ADDR_WID.W)
  val mepc       = 12.U(CSR_ADDR_WID.W)
  val mcause     = 13.U(CSR_ADDR_WID.W)
  val mtval      = 14.U(CSR_ADDR_WID.W)
  val mip        = 15.U(CSR_ADDR_WID.W)

  val addr_default = List(empty, "h00000000_00000000".U, "h00000000_00000000".U)
  
  val addr_table = Array(
    BitPat("b1100_0000_0000") -> List(cycle,      "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    BitPat("b1111_0001_0001") -> List(mvendorid,  "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    BitPat("b1111_0001_0010") -> List(marchid,    "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    BitPat("b1111_0001_0011") -> List(mimpid,     "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    BitPat("b1111_0001_0101") -> List(mhartid,    "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    BitPat("b0011_0000_0000") -> List(mstatus,    "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000088".U),
    BitPat("b0011_0000_0001") -> List(misa,       "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000000".U),
    BitPat("b0011_0000_0100") -> List(mie,        "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat("b0011_0000_0101") -> List(mtvec,      "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat("b0011_0000_0110") -> List(mcounteren, "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat("b0011_0100_0000") -> List(mscratch,   "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat("b0011_0100_0001") -> List(mepc,       "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat("b0011_0100_0010") -> List(mcause,     "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat("b0011_0100_0011") -> List(mtval,      "hFFFFFFFF_FFFFFFFF".U, "hFFFFFFFF_FFFFFFFF".U),
    BitPat("b0011_0100_0100") -> List(mip,        "hFFFFFFFF_FFFFFFFF".U, "h00000000_00000888".U),
  )

  // Initialize Registers
  val regs = RegInit(VecInit(Seq.fill(CREG_NUM)(0.U(XLEN.W))))

  regs(mstatus) := "h00000000_00001800".U
  regs(misa)    := "h80000000_00001100".U

  // Read
  val raddr :: rmask :: r_nop :: Nil =
    ListLookup(io.read.raddr, addr_default, addr_table)

  io.read.rdata := regs(raddr) & rmask

  // Write
  val waddr :: w_nop :: wmask :: Nil =
    ListLookup(io.write.waddr, addr_default, addr_table)

  when (io.write.wen && waddr =/= 0.U) {
    regs(waddr) := (io.write.wdata & wmask) | (regs(waddr) & ~wmask)
  }

}
