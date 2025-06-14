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

  // Stages & Units
  val fetchUnit      = Module(new FetchUnit()).io
  val decodeStage    = Module(new DecodeStage()).io
  val decodeUnit     = Module(new DecodeUnit()).io
<<<<<<< HEAD
=======
  val regfile        = Module(new ARegFile()).io
>>>>>>> parent of 341b062 (lab6F finished.)
  val executeStage   = Module(new ExecuteStage()).io
  val executeUnit    = Module(new ExecuteUnit()).io
  val memoryStage    = Module(new MemoryStage()).io
  val memoryUnit     = Module(new MemoryUnit()).io
  val writeBackStage = Module(new WriteBackStage()).io
  val writeBackUnit  = Module(new WriteBackUnit()).io
<<<<<<< HEAD

  // Controls
  val fetchCtrl      = Module(new FetchCtrl()).io
  val decodeCtrl     = Module(new DecodeCtrl()).io
  val executeCtrl    = Module(new ExecuteCtrl()).io
  val memoryCtrl     = Module(new MemoryCtrl()).io
  val writeBackCtrl  = Module(new WriteBackCtrl()).io
=======
>>>>>>> parent of 341b062 (lab6F finished.)

  // Registers
  val regfile        = Module(new ARegFile()).io
  val csrfile        = Module(new CSRRegFile()).io

  // Pipeline : Stages & Units
  // Fetch -> Decode -> Execute -> Memory -> WriteBack
  fetchUnit.decodeStage <> decodeStage.fetchUnit
  executeUnit.ftc_info <> fetchUnit.ftc_info
  decodeStage.decodeUnit <> decodeUnit.decodeStage
  decodeUnit.executeStage <> executeStage.decodeUnit
  executeStage.executeUnit <> executeUnit.executeStage
  executeUnit.memoryStage <> memoryStage.executeUnit
  memoryStage.memoryUnit <> memoryUnit.memoryStage
  memoryUnit.writeBackStage <> writeBackStage.memoryUnit
  writeBackStage.writeBackUnit <> writeBackUnit.writeBackStage

  // Difftest
  writeBackUnit.debug <> io.debug
  
  // ARegFile
  decodeUnit.regfile <> regfile.read
  writeBackUnit.regfile <> regfile.write

  // CSRRegFile
  executeUnit.csr_read <> csrfile.read
  executeUnit.csr_write <> csrfile.write
  executeUnit.mode <> csrfile.mode

<<<<<<< HEAD
  // Exceptions & Interruptions
  io.interrupt <> csrfile.ext_int
  executeUnit.ex <> csrfile.ex
  executeUnit.mret <> csrfile.mret
  executeUnit.pc <> csrfile.pc
  executeUnit.csr_ftc_info <> csrfile.ftc_info
  decodeUnit.interrupt <> csrfile.interrupt

  // InstSram
  fetchUnit.instSram <> io.instSram

  // DataSram
  executeUnit.dataSram <> io.dataSram
  memoryUnit.loadData := io.dataSram.rdata

  // Control Line
  fetchCtrl.decodeReady <> decodeCtrl.decodeReady
  decodeCtrl.executeReady <> executeCtrl.executeReady
  executeCtrl.memoryReady <> memoryCtrl.memoryReady
  memoryCtrl.writeBackReady <> writeBackCtrl.writeBackReady

  // Control -> Stage
  fetchCtrl.fetchCtrlSignal <> fetchUnit.fetchCtrlSignal
  fetchCtrl.fetchCtrlSignal <> decodeStage.fetchCtrlSignal
  decodeCtrl.decodeCtrlSignal <> executeStage.decodeCtrlSignal
  executeCtrl.executeCtrlSignal <> memoryStage.executeCtrlSignal
  memoryCtrl.memoryCtrlSignal <> writeBackStage.memoryCtrlSignal

  // Control Conflicts : Excute -> ftc_info -> Fetch, Decode
  fetchCtrl.ftc_info <> executeUnit.ftc_info
  decodeCtrl.ftc_info <> executeUnit.ftc_info

  // Data Conflicts (Read after Write) :  Decode, Excute -> info -> Decode
  decodeCtrl.decodeInfo := decodeUnit.executeStage.data.info
  decodeCtrl.executeInfo := executeUnit.memoryStage.data.info
=======
  // LAB6: Ctrl
  val fetchCtrlSignal   = Wire(new CtrlSignal())
  val decodeCtrlSignal  = Wire(new CtrlSignal())
  val executeCtrlSignal = Wire(new CtrlSignal())
  val memoryCtrlSignal  = Wire(new CtrlSignal())

  val f_allow           = Wire(Bool())
  val d_allow           = Wire(Bool())
  val e_allow           = Wire(Bool())
  val m_allow           = Wire(Bool())

  val f_flush           = Wire(Bool())
  val d_flush           = Wire(Bool())
  val e_flush           = Wire(Bool())
  val m_flush           = Wire(Bool())

  val ftcInfo           = executeUnit.ftcInfo
  val d_info            = decodeUnit.executeStage.data.info
  val e_info            = executeUnit.memoryStage.data.info
  val m_info            = memoryUnit.writeBackStage.data.info
  val w_info            = writeBackUnit.writeBackStage.data.info

  f_allow := true.B
  f_flush := ftcInfo.branch

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

  d_allow := !e_conflict && !m_conflict && !w_conflict
  d_flush := ftcInfo.branch

  e_allow := true.B
  e_flush := false.B

  m_allow := true.B
  m_flush := false.B

  fetchCtrlSignal.allow_to_go := f_allow && d_allow
  fetchCtrlSignal.do_flush := f_flush || (!f_allow && d_allow)

  decodeCtrlSignal.allow_to_go := d_allow && e_allow
  decodeCtrlSignal.do_flush := d_flush || (!d_allow && e_allow)

  executeCtrlSignal.allow_to_go := e_allow && m_allow
  executeCtrlSignal.do_flush := e_flush || (!e_allow && m_allow)

  memoryCtrlSignal.allow_to_go := m_allow
  memoryCtrlSignal.do_flush := m_flush

  fetchCtrlSignal <> fetchUnit.fetchCtrlSignal
  fetchCtrlSignal <> decodeStage.fetchCtrlSignal
  decodeCtrlSignal <> executeStage.decodeCtrlSignal
  executeCtrlSignal <> memoryStage.executeCtrlSignal
  memoryCtrlSignal <> writeBackStage.memoryCtrlSignal
>>>>>>> parent of 341b062 (lab6F finished.)

  // Forward 
  decodeUnit.executeInfo := executeUnit.memoryStage.data.info
  decodeUnit.memoryInfo := memoryUnit.writeBackStage.data.info
  decodeUnit.writeBackInfo := writeBackUnit.writeBackStage.data.info
  decodeUnit.executeRdInfo := executeUnit.memoryStage.data.rd_info
  decodeUnit.memoryRdInfo := memoryUnit.writeBackStage.data.rd_info
  decodeUnit.writeBackRdInfo := writeBackUnit.writeBackStage.data.rd_info
}
