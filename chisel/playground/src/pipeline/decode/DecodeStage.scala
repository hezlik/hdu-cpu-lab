package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._
import cpu.CpuConfig

class IfIdData extends Bundle {
  val inst  = UInt(XLEN.W)
  val valid = Bool()
  val pc    = UInt(XLEN.W)
}

class FetchUnitDecodeUnit extends Bundle {
  val data = Output(new IfIdData())
}

class DecodeStage extends Module {
  val io = IO(new Bundle {
    val fetchUnit   = Flipped(new FetchUnitDecodeUnit())
    val decodeUnit  = new FetchUnitDecodeUnit()
    // LAB6: New Input : fetchCtrlSignal
    val fetchCtrlSignal = Input(new CtrlSignal())
  })

  val data = RegInit(0.U.asTypeOf(new IfIdData()))

  // data := io.fetchUnit.data

  // LAB6: fetchCtrlSignal
  when (io.fetchCtrlSignal.allow_to_go) { data := io.fetchUnit.data }
  when (io.fetchCtrlSignal.do_flush) { data.valid := false.B }

  io.decodeUnit.data := data
}
