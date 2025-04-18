package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class Decoder extends Module with HasInstrType {
  val io = IO(new Bundle {
    // inputs
    val in = Input(new Bundle {
      val inst = UInt(XLEN.W)
    })
    // outputs
    val out = Output(new Bundle {
      val info = new Info()
    })
  })

  val inst = io.in.inst
  // 根据输入的指令inst从Instructions.DecodeTable中查找对应的指令类型、功能单元类型和功能单元操作类型
  // 如果找不到匹配的指令，则使用Instructions.DecodeDefault作为默认值
  // instrType、fuType和fuOpType分别被赋值为Instructions.DecodeTable中的对应值
  val instrType :: fuType :: fuOpType :: Nil =
    ListLookup(inst, Instructions.DecodeDefault, Instructions.DecodeTable)

  val (rs, rt, rd) = (inst(19, 15), inst(24, 20), inst(11, 7))

  // TODO: 完成Decoder模块的逻辑
  // io.out.info.valid      :=
  // io.out.info.src1_raddr := 
  // io.out.info.src2_raddr :=
  // io.out.info.op         :=
  // io.out.info.reg_wen    :=
  // io.out.info.reg_waddr  :=

  // LAB1: Decoder
  io.out.info.valid      := instrType =/= InstrN
  io.out.info.src1_raddr := rs
  io.out.info.src2_raddr := rt
  io.out.info.op         := fuOpType
  io.out.info.reg_wen    := (instrType =/= InstrS) && (instrType =/= InstrB)
  io.out.info.reg_waddr  := rd

  // LAB2: Decoder
  io.out.info.src1_ren   := (instrType =/= InstrU) && (instrType =/= InstrJ)
  io.out.info.src2_ren   := (instrType =/= InstrI) && (instrType =/= InstrU) && (instrType =/= InstrJ)
  // io.out.info.src1_pcen  := inst === BitPat("b????????????????????_?????_0010111")

  // LAB5: Decoder : src1_pcen
  io.out.info.src1_pcen  := (inst === BitPat("b????????????????????_?????_0010111")) || (instrType === InstrJ)

  // LAB2: Decoder : imm : InstrI & InstrU
  val imm = Wire(UInt(XLEN.W))
  imm := 0.U
  switch (instrType) {
    is (InstrI) {
      val imm12 = inst(31, 20)
      imm := Cat(Fill(54, imm12(11)), imm12)
    }
    is (InstrU) {
      val imm32 = inst(31, 12) << 12
      imm := Cat(Fill(32, imm32(31)), imm32)
    }
    // LAB4: Decoder : imm : InstrS
    is (InstrS) {
      val imm12 = Cat(inst(31, 25), inst(11, 7))
      imm := Cat(Fill(54, imm12(11)), imm12)
    }
    // LAB5: Decoder : imm : InstrB & InstrJ
    is (InstrB) {
      val imm13 = Cat(Cat(inst(31), inst(7)), Cat(inst(30, 25), inst(11, 8))) << 1
      imm := Cat(Fill(53, imm13(12)), imm13)
    }
    is (InstrJ) {
      val imm21 = Cat(Cat(inst(31), inst(19, 12)), Cat(inst(20), inst(30 ,21))) << 1
      imm := Cat(Fill(43, imm21(20)), imm21)
    }
  }
  io.out.info.imm := imm

  // LAB3: Decoder
  io.out.info.fusel      := fuType

}
