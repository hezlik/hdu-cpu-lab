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
  val executeStage   = Module(new ExecuteStage()).io
  val executeUnit    = Module(new ExecuteUnit()).io
  val memoryStage    = Module(new MemoryStage()).io
  val memoryUnit     = Module(new MemoryUnit()).io
  val writeBackStage = Module(new WriteBackStage()).io
  val writeBackUnit  = Module(new WriteBackUnit()).io

  // Controls
  val fetchCtrl      = Module(new FetchCtrl()).io
  val decodeCtrl     = Module(new DecodeCtrl()).io
  val executeCtrl    = Module(new ExecuteCtrl()).io
  val memoryCtrl     = Module(new MemoryCtrl()).io
  val writeBackCtrl  = Module(new WriteBackCtrl()).io

  // Registers
  val regfile        = Module(new ARegFile()).io
  val csrfile        = Module(new CSRRegFile()).io

  // Pipeline : Stages & Units
  // Fetch -> Decode -> Execute -> Memory -> WriteBack
  // TODO: Split ExceptionUnit
  fetchUnit.decodeStage <> decodeStage.fetchUnit
  executeUnit.ftcInfo <> fetchUnit.ftcInfo
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
  decodeUnit.mode <> csrfile.mode

  // Exceptions & Interruptions
  io.interrupt <> csrfile.ext_int
  executeUnit.ex <> csrfile.ex
  executeUnit.mret <> csrfile.mret
  executeUnit.pc <> csrfile.pc
  executeUnit.csr_ftcInfo <> csrfile.ftcInfo
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

  // Control Conflicts : Excute -> ftcInfo -> Fetch, Decode
  fetchCtrl.ftcInfo <> executeUnit.ftcInfo
  decodeCtrl.ftcInfo <> executeUnit.ftcInfo

  // Data Conflicts (Read after Write) :  Decode, Excute -> info -> Decode
  decodeCtrl.decodeInfo := decodeUnit.executeStage.data.info
  decodeCtrl.executeInfo := executeUnit.memoryStage.data.info

  // Forward 
  decodeUnit.executeInfo := executeUnit.memoryStage.data.info
  decodeUnit.memoryInfo := memoryUnit.writeBackStage.data.info
  decodeUnit.writeBackInfo := writeBackUnit.writeBackStage.data.info
  decodeUnit.executeRdInfo := executeUnit.memoryStage.data.rd_info
  decodeUnit.memoryRdInfo := memoryUnit.writeBackStage.data.rd_info
  decodeUnit.writeBackRdInfo := writeBackUnit.writeBackStage.data.rd_info
}
