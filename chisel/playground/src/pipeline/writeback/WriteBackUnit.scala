package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._
import cpu.CpuConfig

class WriteBackUnit extends Module {
  val io = IO(new Bundle {
    val writeBackStage = Input(new MemoryUnitWriteBackUnit())
    val regfile        = Output(new RegWrite())
    val debug          = new DEBUG()
  })

  // 写回阶段完成数据的写回操作
  // 同时该阶段还负责差分测试的比对工作
  // TODO: 完成WriteBackUnit模块的逻辑

  // LAB1: WriteBack -> data -> Register
  val info    = io.writeBackStage.data.info
  val rd_info = io.writeBackStage.data.rd_info

  io.regfile.wen   := info.valid && info.reg_wen
  io.regfile.waddr := info.reg_waddr
  io.regfile.wdata := rd_info.wdata

  // LAB1: Difftest : Commit Debug
  io.debug.commit   := info.valid
  io.debug.pc       := io.writeBackStage.data.pc
  io.debug.rf_wnum  := info.reg_waddr
  io.debug.rf_wdata := rd_info.wdata

}
