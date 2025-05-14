package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class DecodeUnit extends Module {
  val io = IO(new Bundle {
    val decodeStage     = Flipped(new FetchUnitDecodeUnit())
    val executeStage    = Output(new DecodeUnitExecuteUnit())

    // Read ARegFile
    val regfile         = new Src12Read()
    
    // Forward Info : Execute, Memory, WriteBack
    val executeInfo     = Input(new Info())
    val memoryInfo      = Input(new Info())
    val writeBackInfo   = Input(new Info())
    val executeRdInfo   = Input(new RdInfo())
    val memoryRdInfo    = Input(new RdInfo())
    val writeBackRdInfo = Input(new RdInfo())
    
    // Read CSRRegFile
    val mode            = Input(UInt(MODE_WID.W))
    val interrupt       = Input(Vec(INT_WID, Bool()))
  })

  // 译码阶段完成指令的译码操作以及源操作数的准备
  val decoder = Module(new Decoder()).io
  decoder.in.inst := io.decodeStage.data.inst

  val pc   = io.decodeStage.data.pc
  val inst = io.decodeStage.data.inst
  val info = Wire(new Info())

  // TODO: Exceptions Delays
  // Temporary ExceptionInfo
  val ex_exc  = WireInit(VecInit(Seq.fill(EXC_WID)(false.B)))
  val ex_int  = WireInit(VecInit(Seq.fill(INT_WID)(false.B)))
  val ex_tval = WireInit(VecInit(Seq.fill(EXC_WID)(0.U(XLEN.W))))
  
  // Exception : instAddrMisaligned
  when (info.valid && pc(1,0) =/= "b00".U) {
    ex_exc(instAddrMisaligned)  := true.B
    ex_tval(instAddrMisaligned) := pc
  }

  // Exception : illegalInst
  when (info.valid && info.illegal) {
    ex_exc(illegalInst)  := true.B
    ex_tval(illegalInst) := inst
  }

  // Exception : breakPoint
  when (info.valid && info.fusel === FuType.csr && info.op === CSROpType.ebreak) {
    ex_exc(breakPoint)  := true.B
  }

  // Exception : ecallU, ecallS, ecallM
  when (info.valid && info.fusel === FuType.csr && info.op === CSROpType.ecall) {
    switch (io.mode) {
      is (Privilege.u) { ex_exc(ecallU) := true.B }
      is (Privilege.s) { ex_exc(ecallS) := true.B }
      is (Privilege.m) { ex_exc(ecallM) := true.B }
    }
  }

  // TODO: valid
  info       := decoder.out.info
  info.valid := io.decodeStage.data.valid

  // Read ARegFile
  io.regfile.src1.raddr := info.src1_raddr
  io.regfile.src2.raddr := info.src2_raddr

  io.executeStage.data.pc   := pc
  io.executeStage.data.info := info

  // Forward Preparation
  def Conflict_1(r_info : Info, w_info : Info) = {
    w_info.valid && w_info.reg_wen && w_info.reg_waddr =/= 0.U &&
    r_info.src1_ren && r_info.src1_raddr === w_info.reg_waddr
  }

  def Conflict_2(r_info : Info, w_info : Info) = {
    w_info.valid && w_info.reg_wen && w_info.reg_waddr =/= 0.U &&
    r_info.src2_ren && r_info.src2_raddr === w_info.reg_waddr
  }

  val e_info  = io.executeInfo
  val m_info  = io.memoryInfo
  val w_info  = io.writeBackInfo
  val e_wdata = io.executeRdInfo.wdata
  val m_wdata = io.memoryRdInfo.wdata
  val w_wdata = io.writeBackRdInfo.wdata

  // src1_data Reuse
  val src1_table = IndexedSeq(
    info.is_csri             -> info.zimm,
    Conflict_1(info, e_info) -> e_wdata,
    Conflict_1(info, m_info) -> m_wdata,
    Conflict_1(info, w_info) -> w_wdata,
    info.src1_ren            -> io.regfile.src1.rdata,
    info.src1_pcen           -> pc,
  )
  io.executeStage.data.src_info.src1_data := MuxCase(0.U, src1_table)
  
  // src2_data Reuse
  val src2_table = IndexedSeq(
    Conflict_2(info, e_info) -> e_wdata,
    Conflict_2(info, m_info) -> m_wdata,
    Conflict_2(info, w_info) -> w_wdata,
    info.src2_ren            -> io.regfile.src2.rdata,
  )
  io.executeStage.data.src_info.src2_data := MuxCase(info.imm, src2_table)

  io.executeStage.data.ex.exception := ex_exc
  io.executeStage.data.ex.tval      := ex_tval

  // Merge Interruptions from CSRRegFile
  io.executeStage.data.ex.interrupt :=
    ex_int.zip(io.interrupt).map { 
    case (ex, intr) => ex || intr 
  }
}
