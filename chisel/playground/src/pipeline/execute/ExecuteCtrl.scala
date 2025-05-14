package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class ExecuteCtrl extends Module {
  val io = IO(new Bundle {
    val memoryReady       = Input(Bool())
    val executeReady      = Output(Bool())
    val executeCtrlSignal = Output(new CtrlSignal())
  })

  val ready = io.memoryReady
  val allow = true.B
  val flush = false.B

  io.executeCtrlSignal.allow_to_go := allow && ready
  io.executeCtrlSignal.do_flush    := flush || (!allow && ready)

  io.executeReady := io.executeCtrlSignal.allow_to_go
}
