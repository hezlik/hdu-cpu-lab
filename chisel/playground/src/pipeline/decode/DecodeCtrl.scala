// LAB6F: DecodeCtrl

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

    val ftcInfo          = Input(new FetchInfo())

    val decodeInfo       = Input(new Info())
    val executeInfo      = Input(new Info())
    val memoryInfo       = Input(new Info())
    val writeBackInfo    = Input(new Info())
  })

  val ready = io.executeReady
  val flush = io.ftcInfo.branch

  val d_info = io.decodeInfo
  val e_info = io.executeInfo
  val m_info = io.memoryInfo
  val w_info = io.writeBackInfo

  val e_conflict =
    e_info.valid && e_info.reg_wen && e_info.reg_waddr =/= 0.U && ((
      d_info.src1_ren && d_info.src1_raddr === e_info.reg_waddr
    ) || (
      d_info.src2_ren && d_info.src2_raddr === e_info.reg_waddr
    ))

  val m_conflict =
    m_info.valid && m_info.reg_wen && m_info.reg_waddr =/= 0.U && ((
      d_info.src1_ren && d_info.src1_raddr === m_info.reg_waddr
    ) || (
      d_info.src2_ren && d_info.src2_raddr === m_info.reg_waddr
    ))

  val w_conflict =
    w_info.valid && w_info.reg_wen && w_info.reg_waddr =/= 0.U && ((
      d_info.src1_ren && d_info.src1_raddr === w_info.reg_waddr
    ) || (
      d_info.src2_ren && d_info.src2_raddr === w_info.reg_waddr
    ))

  val allow = !e_conflict && !m_conflict && !w_conflict

  io.decodeCtrlSignal.allow_to_go := allow && ready
  io.decodeCtrlSignal.do_flush := flush || (!allow && ready)

  io.decodeReady := io.decodeCtrlSignal.allow_to_go

}
