package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.CpuConfig
import cpu.defines._
import cpu.defines.Const._
import chisel3.util.experimental.BoringUtils

class ExecuteUnit extends Module {
  val io = IO(new Bundle {
    val executeStage = Input(new DecodeUnitExecuteUnit())
    val memoryStage  = Output(new ExecuteUnitMemoryUnit())
    val dataSram     = new DataSram()
    // LAB5: ExecuteUnit New Output : ftcInfo
    val ftcInfo      = Output(new FetchInfo())
    // LAB8: ExecuteUnit New Interaction : csr_read & csr_write
    val csr_read     = new CsrRead()
    val csr_write    = new CsrWrite()
    // LAB9: ExecuteUnit New Output : ExceptionInfo, mret, pc
    val ex           = Output(new ExceptionInfo())
    val mret         = Output(Bool())
    val pc           = Output(UInt(XLEN.W))
    // LAB9: ExecuteUnit New Input : csr_ftcInfo
    val csr_ftcInfo  = Input(new FetchInfo())
  })

  // 执行阶段完成指令的执行操作

  val fu = Module(new Fu()).io
  fu.data.pc       := io.executeStage.data.pc
  fu.data.info     := io.executeStage.data.info
  fu.data.src_info := io.executeStage.data.src_info

  io.dataSram <> fu.dataSram

  // TODO: 完成ExecuteUnit模块的逻辑
  // io.memoryStage.data.pc       := 
  // io.memoryStage.data.info     := 
  // io.memoryStage.data.src_info := 
  // io.memoryStage.data.rd_info  := 

  // LAB1: ExecuteUnit
  io.memoryStage.data.pc       := fu.data.pc
  io.memoryStage.data.info     := fu.data.info
  io.memoryStage.data.src_info := fu.data.src_info
  io.memoryStage.data.rd_info  := fu.data.rd_info

  // LAB5: ExecuteUnit : ftcInfo
  io.ftcInfo <> fu.ftcInfo

  // LAB8: ExecuteUnit : csr_read & csr_write
  io.csr_read <> fu.csr_read
  io.csr_write <> fu.csr_write

  // LAB9: ExecuteUnit : ExceptionInfo
  io.executeStage.data.ex <> fu.ex_in

  when (io.memoryStage.data.info.valid) {
    io.ex <> fu.ex_out
  }.otherwise {
    io.ex.exception := VecInit(Seq.fill(EXC_WID)(false.B))
    io.ex.interrupt := VecInit(Seq.fill(INT_WID)(false.B))
    io.ex.tval      := VecInit(Seq.fill(EXC_WID)(0.U(XLEN.W)))
  }

  // LAB9: csrfile -> csr_ftcInfo -> ExecuteUnit
  when (io.csr_ftcInfo.flush) {
    io.ftcInfo := io.csr_ftcInfo
    io.memoryStage.data.info.reg_wen := false.B
  }

  // LAB9: ExecuteUnit : mret, pc
  io.mret <> fu.mret
  io.pc := fu.data.pc
  
}
