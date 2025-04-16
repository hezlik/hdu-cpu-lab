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
  val decodeStage    = Module(new DecodeStage()).io
  val decodeUnit     = Module(new DecodeUnit()).io
  val regfile        = Module(new ARegFile()).io
  val executeStage   = Module(new ExecuteStage()).io
  val executeUnit    = Module(new ExecuteUnit()).io
  val memoryStage    = Module(new MemoryStage()).io
  val memoryUnit     = Module(new MemoryUnit()).io
  val writeBackStage = Module(new WriteBackStage()).io
  val writeBackUnit  = Module(new WriteBackUnit()).io

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

}
