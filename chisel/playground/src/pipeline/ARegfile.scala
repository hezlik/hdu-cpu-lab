package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._
import cpu.CpuConfig

class SrcRead extends Bundle {
  val raddr = Output(UInt(REG_ADDR_WID.W))
  val rdata = Input(UInt(XLEN.W))
}

class Src12Read extends Bundle {
  val src1 = new SrcRead()
  val src2 = new SrcRead()
}

class RegWrite extends Bundle {
  val wen   = Output(Bool())
  val waddr = Output(UInt(REG_ADDR_WID.W))
  val wdata = Output(UInt(XLEN.W))
}

class ARegFile extends Module {
  val io = IO(new Bundle {
    val read  = Flipped(new Src12Read())
    val write = Flipped(new RegWrite())
  })
  
  val regs = RegInit(VecInit(Seq.fill(AREG_NUM)(0.U(XLEN.W))))
  
  // Read
  io.read.src1.rdata := regs(io.read.src1.raddr)
  io.read.src2.rdata := regs(io.read.src2.raddr)

  // Write
  // Notice : regs(0) = 0 !
  when (io.write.wen && (io.write.waddr =/= 0.U)) {
    regs(io.write.waddr) := io.write.wdata
  }
}
