package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._
import cpu.CpuConfig

class MemWbData extends Bundle {
  val pc      = UInt(XLEN.W)
  val info    = new Info()
  val rd_info = new RdInfo()
}

class MemoryUnitWriteBackUnit extends Bundle {
  val data = new MemWbData()
}
class WriteBackStage extends Module {
  val io = IO(new Bundle {
    val memoryUnit     = Input(new MemoryUnitWriteBackUnit())
    val writeBackUnit  = Output(new MemoryUnitWriteBackUnit())
    // LAB6: New Input : memoryCtrlSignal
    val memoryCtrlSignal = Input(new CtrlSignal())
  })

  val data = RegInit(0.U.asTypeOf(new MemWbData()))
  
  // TODO: 完成WriteBackStage模块的逻辑

  // LAB6: memoryCtrlSignal
  when (io.memoryCtrlSignal.allow_to_go) { data := io.memoryUnit.data }
  when (io.memoryCtrlSignal.do_flush) { data.info.valid := false.B }

  // LAB1: WriteBackStage
  // data := io.memoryUnit.data
  io.writeBackUnit.data := data
  
}
