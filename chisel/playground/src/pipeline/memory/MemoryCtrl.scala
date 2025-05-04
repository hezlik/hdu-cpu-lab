// LAB6F: MemoryCtrl

package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class MemoryCtrl extends Module {
  val io = IO(new Bundle {
    val writeBackReady   = Input(Bool())
    val memoryReady      = Output(Bool())
    val memoryCtrlSignal = Output(new CtrlSignal())
  })

  val ready = io.writeBackReady
  val allow = true.B
  val flush = false.B

  io.memoryCtrlSignal.allow_to_go := allow && ready
  io.memoryCtrlSignal.do_flush := flush || (!allow && ready)

  io.memoryReady := io.memoryCtrlSignal.allow_to_go

}
