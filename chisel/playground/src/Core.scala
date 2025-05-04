package cpu

import chisel3._
import chisel3.util._

import defines._
import defines.Const._
import pipeline._

class Core extends Module {
  val io = IO(new Bundle {
    val interrupt = Input(new ExtInterrupt())
    val instSram  = new InstSram()
    val dataSram  = new DataSram()
    val debug     = new DEBUG()
  })

  val fetchUnit      = Module(new FetchUnit()).io
  val fetchCtrl      = Module(new FetchCtrl()).io
  val decodeStage    = Module(new DecodeStage()).io
  val decodeUnit     = Module(new DecodeUnit()).io
  val decodeCtrl     = Module(new DecodeCtrl()).io
  val regfile        = Module(new ARegFile()).io
  val executeStage   = Module(new ExecuteStage()).io
  val executeUnit    = Module(new ExecuteUnit()).io
  val executeCtrl    = Module(new ExecuteCtrl()).io
  val memoryStage    = Module(new MemoryStage()).io
  val memoryUnit     = Module(new MemoryUnit()).io
  val memoryCtrl     = Module(new MemoryCtrl()).io
  val writeBackStage = Module(new WriteBackStage()).io
  val writeBackUnit  = Module(new WriteBackUnit()).io
  val writeBackCtrl  = Module(new WriteBackCtrl()).io

  // 取指单元
  fetchUnit.instSram <> io.instSram
  fetchUnit.decodeStage <> decodeStage.fetchUnit

  // TODO: 完成Core模块的逻辑
  // 在该模块中，需要将各个模块连接起来，形成一个完整的CPU核心

  // LAB5: FetchUnit
  executeUnit.ftcInfo <> fetchUnit.ftcInfo

  // LAB1: Decode
  decodeStage.decodeUnit <> decodeUnit.decodeStage
  decodeUnit.regfile <> regfile.read
  decodeUnit.executeStage <> executeStage.decodeUnit

  // LAB1: Execute
  executeStage.executeUnit <> executeUnit.executeStage
  executeUnit.dataSram <> io.dataSram
  executeUnit.memoryStage <> memoryStage.executeUnit

  // LAB1: Memory
  memoryStage.memoryUnit <> memoryUnit.memoryStage
  memoryUnit.writeBackStage <> writeBackStage.memoryUnit

  // LAB4: Memory
  memoryUnit.loadData := io.dataSram.rdata

  // LAB1: Writeback
  writeBackStage.writeBackUnit <> writeBackUnit.writeBackStage
  writeBackUnit.regfile <> regfile.write

  // LAB1: Difftest
  writeBackUnit.debug <> io.debug

  // LAB6F: Ready
  fetchCtrl.decodeReady <> decodeCtrl.decodeReady
  decodeCtrl.executeReady <> executeCtrl.executeReady
  executeCtrl.memoryReady <> memoryCtrl.memoryReady
  memoryCtrl.writeBackReady <> writeBackCtrl.writeBackReady

  // LAB6F: Ctrl
  fetchCtrl.fetchCtrlSignal <> fetchUnit.fetchCtrlSignal
  fetchCtrl.fetchCtrlSignal <> decodeStage.fetchCtrlSignal
  decodeCtrl.decodeCtrlSignal <> executeStage.decodeCtrlSignal
  executeCtrl.executeCtrlSignal <> memoryStage.executeCtrlSignal
  memoryCtrl.memoryCtrlSignal <> writeBackStage.memoryCtrlSignal

  // LAB6F: Excute -> ftcInfo -> Fetch, Decode
  fetchCtrl.ftcInfo <> executeUnit.ftcInfo
  decodeCtrl.ftcInfo <> executeUnit.ftcInfo

  // LAB6F: Decode, Excute, Memory, WriteBack -> info -> Decode
  decodeCtrl.decodeInfo := decodeUnit.executeStage.data.info
  decodeCtrl.executeInfo := executeUnit.memoryStage.data.info
  decodeCtrl.memoryInfo := memoryUnit.writeBackStage.data.info
  decodeCtrl.writeBackInfo := writeBackUnit.writeBackStage.data.info

  // LAB7: Excute, Memory, WriteBack -> info & rdinfo-> Decode
  decodeUnit.executeInfo := executeUnit.memoryStage.data.info
  decodeUnit.memoryInfo := memoryUnit.writeBackStage.data.info
  decodeUnit.writeBackInfo := writeBackUnit.writeBackStage.data.info
  decodeUnit.executeRdInfo := executeUnit.memoryStage.data.rd_info
  decodeUnit.memoryRdInfo := memoryUnit.writeBackStage.data.rd_info
  decodeUnit.writeBackRdInfo := writeBackUnit.writeBackStage.data.rd_info

  // LAB6C: Ctrl
  // val fetchCtrlSignal   = Wire(new CtrlSignal())
  // val decodeCtrlSignal  = Wire(new CtrlSignal())
  // val executeCtrlSignal = Wire(new CtrlSignal())
  // val memoryCtrlSignal  = Wire(new CtrlSignal())

  // val f_allow           = Wire(Bool())
  // val d_allow           = Wire(Bool())
  // val e_allow           = Wire(Bool())
  // val m_allow           = Wire(Bool())

  // val f_flush           = Wire(Bool())
  // val d_flush           = Wire(Bool())
  // val e_flush           = Wire(Bool())
  // val m_flush           = Wire(Bool())

  // val ftcInfo           = executeUnit.ftcInfo
  // val d_info            = decodeUnit.executeStage.data.info
  // val e_info            = executeUnit.memoryStage.data.info
  // val m_info            = memoryUnit.writeBackStage.data.info
  // val w_info            = writeBackUnit.writeBackStage.data.info

  // f_allow := true.B
  // f_flush := ftcInfo.branch

  // val e_conflict =
  //   e_info.valid && e_info.reg_wen && e_info.reg_waddr =/= 0.U && ((
  //     d_info.src1_ren && d_info.src1_raddr === e_info.reg_waddr
  //   ) || (
  //     d_info.src2_ren && d_info.src2_raddr === e_info.reg_waddr
  //   ))

  // val m_conflict =
  //   m_info.valid && m_info.reg_wen && m_info.reg_waddr =/= 0.U && ((
  //     d_info.src1_ren && d_info.src1_raddr === m_info.reg_waddr
  //   ) || (
  //     d_info.src2_ren && d_info.src2_raddr === m_info.reg_waddr
  //   ))

  // val w_conflict =
  //   w_info.valid && w_info.reg_wen && w_info.reg_waddr =/= 0.U && ((
  //     d_info.src1_ren && d_info.src1_raddr === w_info.reg_waddr
  //   ) || (
  //     d_info.src2_ren && d_info.src2_raddr === w_info.reg_waddr
  //   ))

  // d_allow := !e_conflict && !m_conflict && !w_conflict
  // d_flush := ftcInfo.branch

  // e_allow := true.B
  // e_flush := false.B

  // m_allow := true.B
  // m_flush := false.B

  // fetchCtrlSignal.allow_to_go := f_allow && d_allow
  // fetchCtrlSignal.do_flush := f_flush || (!f_allow && d_allow)

  // decodeCtrlSignal.allow_to_go := d_allow && e_allow
  // decodeCtrlSignal.do_flush := d_flush || (!d_allow && e_allow)

  // executeCtrlSignal.allow_to_go := e_allow && m_allow
  // executeCtrlSignal.do_flush := e_flush || (!e_allow && m_allow)

  // memoryCtrlSignal.allow_to_go := m_allow
  // memoryCtrlSignal.do_flush := m_flush

  // fetchCtrlSignal <> fetchUnit.fetchCtrlSignal
  // fetchCtrlSignal <> decodeStage.fetchCtrlSignal
  // decodeCtrlSignal <> executeStage.decodeCtrlSignal
  // executeCtrlSignal <> memoryStage.executeCtrlSignal
  // memoryCtrlSignal <> writeBackStage.memoryCtrlSignal

}
