package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class Decoder extends Module with HasInstrType {
  val io = IO(new Bundle {
    val inst = Input(UInt(XLEN.W))
    val info = Output(new Info())
  })

  val inst = io.inst
  // 根据输入的指令inst从Instructions.DecodeTable中查找对应的指令类型、功能单元类型和功能单元操作类型
  // 如果找不到匹配的指令，则使用Instructions.DecodeDefault作为默认值
  // instrType、fuType和fuOpType分别被赋值为Instructions.DecodeTable中的对应值
  val instrType :: fuType :: fuOpType :: Nil =
    ListLookup(inst, Instructions.DecodeDefault, Instructions.DecodeTable)

  val (rs, rt, rd) = (inst(19, 15), inst(24, 20), inst(11, 7))

  io.info.valid      := instrType =/= InstrN
  io.info.inst       := inst
  io.info.op         := fuOpType
  io.info.fusel      := fuType
  io.info.src1_ren   := (instrType =/= InstrU) && (instrType =/= InstrJ)
  io.info.src1_pcen  := (inst === RV32I_ALUInstr.AUIPC) || (instrType === InstrJ)
  io.info.src1_raddr := rs
  io.info.src2_ren   := (instrType =/= InstrI) && (instrType =/= InstrU) && (instrType =/= InstrJ)
  io.info.src2_raddr := rt
  io.info.reg_wen    := (instrType =/= InstrS) && (instrType =/= InstrB) && !(fuType === FuType.alu && fuOpType === ALUOpType.nop)
  io.info.reg_waddr  := rd
  io.info.csr        := inst(31, 20)
  io.info.is_csri    := fuType === FuType.csr && CSROpType.isCSRI(inst)
  io.info.zimm       := inst(19, 15)

  val imm = WireInit(0.U(XLEN.W))

  switch (instrType) {
    is (InstrI) {
      val imm12 = inst(31, 20)
      imm := Cat(Fill(XLEN - 12, imm12(11)), imm12)
    }
    is (InstrU) {
      val imm32 = inst(31, 12) << 12
      imm := Cat(Fill(XLEN - 32, imm32(31)), imm32)
    }
    is (InstrS) {
      val imm12 = Cat(inst(31, 25), inst(11, 7))
      imm := Cat(Fill(XLEN - 12, imm12(11)), imm12)
    }
    is (InstrB) {
      val imm13 = Cat(Cat(inst(31), inst(7)), Cat(inst(30, 25), inst(11, 8))) << 1
      imm := Cat(Fill(XLEN - 13, imm13(12)), imm13)
    }
    is (InstrJ) {
      val imm21 = Cat(Cat(inst(31), inst(19, 12)), Cat(inst(20), inst(30 ,21))) << 1
      imm := Cat(Fill(XLEN - 20, imm21(20)), imm21)
    }
  }

  io.info.imm := imm
}
