package cpu.pipeline

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils
import cpu.defines._
import cpu.defines.Const._
import cpu.CpuConfig

class MemoryUnit extends Module {
  val io = IO(new Bundle {
    val memoryStage    = Input(new ExecuteUnitMemoryUnit())
    val writeBackStage = Output(new MemoryUnitWriteBackUnit())

    // LAB4: MemoryUnit : Input loadData
    val loadData       = Input(UInt(XLEN.W))
  })

  // 访存阶段完成指令的访存操作

  io.writeBackStage.data.pc                        := io.memoryStage.data.pc
  io.writeBackStage.data.info                      := io.memoryStage.data.info
  io.writeBackStage.data.rd_info.wdata             := io.memoryStage.data.rd_info.wdata

  // LAB4: MemoryUnit : Finish Load
  val data  = io.memoryStage.data

  val valid = data.info.valid
  val fusel = data.info.fusel
  val is_l  = !data.info.src2_ren
  val op    = data.info.op
  val addr  = data.src_info.src1_data + data.info.imm
  val rdata = io.loadData >> (addr(2, 0) * 8.U)

  val res   = Wire(UInt(XLEN.W))
  
  res := data.rd_info.wdata

  when (valid && fusel === FuType.lsu && is_l) {
    switch (op) {
      is (LSUOpType. lb) { res := Cat(Fill(56, rdata( 7)), rdata( 7, 0)) }
      is (LSUOpType. lh) { res := Cat(Fill(48, rdata(15)), rdata(15, 0)) }
      is (LSUOpType. lw) { res := Cat(Fill(32, rdata(31)), rdata(31, 0)) }
      is (LSUOpType. ld) { res := rdata }
      is (LSUOpType.lbu) { res := rdata( 7, 0) }
      is (LSUOpType.lhu) { res := rdata(15, 0) }
      is (LSUOpType.lwu) { res := rdata(31, 0) }
    }
  }
  
  io.writeBackStage.data.rd_info.wdata := res

}
