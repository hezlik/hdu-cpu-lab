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

    // Read & Write DataSRAM
    val dataSram     = new DataSram()

    // Control Conflict : Commit ftc_info
    val ftc_info     = Output(new FetchInfo())

    // Read & Write CSRRegFile
    val csr_read     = new CsrRead()
    val csr_write    = new CsrWrite()
    val mode         = Input(UInt(MODE_WID.W))

    // Exceptions & Interruptions
    val ex           = Output(new ExceptionInfo())
    val mret         = Output(Bool())
    val pc           = Output(UInt(XLEN.W))
    val csr_ftc_info = Input(new FetchInfo())
  })

  // 执行阶段完成指令的执行操作
  val fu = Module(new Fu()).io
  fu.data.pc       := io.executeStage.data.pc
  fu.data.info     := io.executeStage.data.info
  fu.data.src_info := io.executeStage.data.src_info

  io.dataSram <> fu.dataSram

  io.memoryStage.data.pc       := fu.data.pc
  io.memoryStage.data.info     := fu.data.info
  io.memoryStage.data.src_info := fu.data.src_info
  io.memoryStage.data.rd_info  := fu.data.rd_info

  // Conflict Conflict: Commit ftc_info
  io.ftc_info <> fu.ftc_info

  // Read & Write CSRRegFile
  io.csr_read <> fu.csr_read
  io.csr_write <> fu.csr_write
  io.mode <> fu.mode

  // Exceptions & Interruptions
  // Commit ex, mret, pc
  io.executeStage.data.ex <> fu.old_ex

  when (io.memoryStage.data.info.valid) {
    io.ex <> fu.new_ex
  }.otherwise {
    io.ex.exception := VecInit(Seq.fill(EXC_WID)(false.B))
    io.ex.interrupt := VecInit(Seq.fill(INT_WID)(false.B))
    io.ex.tval      := VecInit(Seq.fill(EXC_WID)(0.U(XLEN.W)))
  }

  io.mret <> fu.mret
  io.pc := fu.data.pc

  // Accept csr_ftc_info
  when (io.csr_ftc_info.flush) {
    io.ftc_info := io.csr_ftc_info
    io.memoryStage.data.info.reg_wen := false.B
  }
  
}
