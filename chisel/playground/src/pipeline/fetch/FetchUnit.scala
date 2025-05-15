package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines.Const._
import cpu.CpuConfig
import cpu.defines._

class FetchUnit extends Module {
  val io = IO(new Bundle {
    val decodeStage     = new FetchUnitDecodeUnit()
    val fetchCtrlSignal = Input(new CtrlSignal())

    // Read InstSRAM
    val instSram        = new InstSram()
    
    // Control Conflict
    val ftc_info        = Input(new FetchInfo())
  })

  val boot :: send :: receive :: Nil = Enum(3)
  val state                          = RegInit(boot)

  switch(state) {
    is(boot) {
      state := send
    }
    is(send) {
      state := receive
    }
    is(receive) {}
  }

  // 取指阶段完成指令的取指操作
  val pc = RegEnable(io.instSram.addr, (PC_INIT - 4.U), state =/= boot)

  io.instSram.addr := pc + 4.U

  // Control Conflict : ftc_info
  when (!io.fetchCtrlSignal.allow_to_go) { io.instSram.addr := pc }
  when (io.ftc_info.flush) { io.instSram.addr := io.ftc_info.target }

  io.decodeStage.data.valid := state === receive
  io.decodeStage.data.pc    := pc
  io.decodeStage.data.inst  := io.instSram.rdata

  io.instSram.en    := !reset.asBool
  io.instSram.wen   := 0.U
  io.instSram.wdata := 0.U
}
