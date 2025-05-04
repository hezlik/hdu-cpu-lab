// LAB6F: FetchCtrl

package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class FetchCtrl extends Module {
  val io = IO(new Bundle {
    val decodeReady     = Input(Bool())
    val fetchCtrlSignal = Output(new CtrlSignal())

    val ftcInfo         = Input(new FetchInfo())
  })

  val ready = io.decodeReady
  val allow = true.B
  val flush = io.ftcInfo.branch

  io.fetchCtrlSignal.allow_to_go := allow && ready
  io.fetchCtrlSignal.do_flush := flush || (!allow && ready)

}
