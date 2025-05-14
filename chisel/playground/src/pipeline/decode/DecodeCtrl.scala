package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class DecodeCtrl extends Module {
  val io = IO(new Bundle {
    val executeReady     = Input(Bool())
    val decodeReady      = Output(Bool())
    val decodeCtrlSignal = Output(new CtrlSignal())

    // Contral Conflict
    val ftcInfo          = Input(new FetchInfo())

    // Data Conflict (Read after Write)
    val decodeInfo       = Input(new Info())
    val executeInfo      = Input(new Info())
  })

  val ready = io.executeReady

  // Data Conflict (Read after Write) : Load Instructions
  val d_info = io.decodeInfo
  val e_info = io.executeInfo
  
  val allow = !(
    e_info.valid && e_info.reg_wen && e_info.reg_waddr =/= 0.U &&
    e_info.fusel === FuType.lsu && !e_info.src2_ren && ((
      d_info.src1_ren && d_info.src1_raddr === e_info.reg_waddr
    ) || (
      d_info.src2_ren && d_info.src2_raddr === e_info.reg_waddr
    ))
  )

  // Control Conflict : ftcInfo
  val flush = io.ftcInfo.flush

  io.decodeCtrlSignal.allow_to_go := allow && ready
  io.decodeCtrlSignal.do_flush    := flush || (!allow && ready)

  io.decodeReady := io.decodeCtrlSignal.allow_to_go
}
