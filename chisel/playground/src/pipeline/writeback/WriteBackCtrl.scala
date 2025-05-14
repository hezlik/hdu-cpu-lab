package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class WriteBackCtrl extends Module {
  val io = IO(new Bundle {
    val writeBackReady = Output(Bool())
  })

  io.writeBackReady := true.B
}
