package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._
import cpu.CpuConfig

class IdExeData extends Bundle {
  val pc       = UInt(XLEN.W)
  val info     = new Info()
  val src_info = new SrcInfo()
}

class DecodeUnitExecuteUnit extends Bundle {
  val data = new IdExeData()
}

class ExecuteStage extends Module {
  val io = IO(new Bundle {
    val decodeUnit   = Input(new DecodeUnitExecuteUnit())
    val executeUnit  = Output(new DecodeUnitExecuteUnit())
    // LAB6: New Input : decodeCtrlSignal
    val decodeCtrlSignal = Input(new CtrlSignal())
  })

  val data = RegInit(0.U.asTypeOf(new IdExeData()))

  // TODO: 完成ExecuteStage模块的逻辑

  // LAB6: decodeCtrlSignal
  when (io.decodeCtrlSignal.allow_to_go) { data := io.decodeUnit.data }
  when (io.decodeCtrlSignal.do_flush) { data.info.valid := false.B }

  // LAB1: ExecuteStage
  // data := io.decodeUnit.data
  io.executeUnit.data := data

}
