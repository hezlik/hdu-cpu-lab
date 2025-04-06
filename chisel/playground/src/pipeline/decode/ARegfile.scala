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

  // 定义32个XLEN位寄存器
  // val regs = RegInit(VecInit(Seq.fill(AREG_NUM)(0.U(XLEN.W))))
  
  // LAB1: Initialize Register regs(i) = i
  // val regs = RegInit(VecInit(
  //    0.U(XLEN.W),  1.U(XLEN.W),  2.U(XLEN.W),  3.U(XLEN.W),
  //    4.U(XLEN.W),  5.U(XLEN.W),  6.U(XLEN.W),  7.U(XLEN.W),
  //    8.U(XLEN.W),  9.U(XLEN.W), 10.U(XLEN.W), 11.U(XLEN.W),
  //   12.U(XLEN.W), 13.U(XLEN.W), 14.U(XLEN.W), 15.U(XLEN.W),
  //   16.U(XLEN.W), 17.U(XLEN.W), 18.U(XLEN.W), 19.U(XLEN.W),
  //   20.U(XLEN.W), 21.U(XLEN.W), 22.U(XLEN.W), 23.U(XLEN.W),
  //   24.U(XLEN.W), 25.U(XLEN.W), 26.U(XLEN.W), 27.U(XLEN.W),
  //   28.U(XLEN.W), 29.U(XLEN.W), 30.U(XLEN.W), 31.U(XLEN.W),
  // ))

  // LAB2: Initialize Register regs(i) = 0
  val regs = RegInit(VecInit(Seq.fill(AREG_NUM)(0.U(XLEN.W))))
  
  // 写寄存器堆
  // TODO:完成写寄存器堆逻辑
  // 注意：0号寄存器恒为0

  // LAB1: Register : Write
  when (io.write.wen && (io.write.waddr =/= 0.U)) {
    regs(io.write.waddr) := io.write.wdata
  }
  // 读寄存器堆
  // TODO:完成读寄存器堆逻辑
  // 注意：0号寄存器恒为0

  // LAB1: Register : Read
  io.read.src1.rdata := regs(io.read.src1.raddr)
  io.read.src2.rdata := regs(io.read.src2.raddr)

}
