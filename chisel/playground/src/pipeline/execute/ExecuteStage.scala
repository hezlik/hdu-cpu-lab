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
  val ex       = new ExceptionInfo()
}

class DecodeUnitExecuteUnit extends Bundle {
  val data = new IdExeData()
}

class ExecuteStage extends Module {
  val io = IO(new Bundle {
<<<<<<< HEAD
    val decodeUnit       = Input(new DecodeUnitExecuteUnit())
    val executeUnit      = Output(new DecodeUnitExecuteUnit())
=======
    val decodeUnit  = Input(new DecodeUnitExecuteUnit())
    val executeUnit = Output(new DecodeUnitExecuteUnit())
    // LAB6: New Input : decodeCtrlSignal
>>>>>>> parent of 341b062 (lab6F finished.)
    val decodeCtrlSignal = Input(new CtrlSignal())
  })

  val data = RegInit(0.U.asTypeOf(new IdExeData()))

  when (io.decodeCtrlSignal.allow_to_go) { data := io.decodeUnit.data }
  when (io.decodeCtrlSignal.do_flush) { data.info.valid := false.B }

  io.executeUnit.data := data
}
