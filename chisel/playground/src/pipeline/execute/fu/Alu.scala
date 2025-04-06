package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class Alu extends Module {
  val io = IO(new Bundle {
    val info     = Input(new Info())
    val src_info = Input(new SrcInfo())
    val result   = Output(UInt(XLEN.W))
  })
  // TODO: 完成ALU模块的逻辑

  // LAB1: ALU
  val valid = io.info.valid
  val op    = io.info.op
  val rs    = io.src_info.src1_data
  val rt    = io.src_info.src2_data

  def W(x : UInt) = {
    val x32 = x(31, 0)
    Cat(Fill(32, x32(31)), x32)
  }

  val res = Wire(UInt(XLEN.W))
  res := rs + rt
  when (valid) {
    switch (op) {
      is (ALUOpType. add) { res := rs + rt }
      is (ALUOpType. sub) { res := rs - rt }
      is (ALUOpType. sll) { res := rs << rt(5, 0) }
      is (ALUOpType. slt) { res := (rs.asSInt < rt.asSInt).asUInt }
      is (ALUOpType.sltu) { res := (rs < rt).asUInt }
      is (ALUOpType. xor) { res := rs ^ rt }
      is (ALUOpType. srl) { res := rs >> rt(5, 0) }
      is (ALUOpType. sra) { res := (rs.asSInt >> rt(5, 0)).asUInt }
      is (ALUOpType.  or) { res := rs | rt }
      is (ALUOpType. and) { res := rs & rt }
      is (ALUOpType.addw) { res := W(rs + rt) }
      is (ALUOpType.subw) { res := W(rs - rt) }
      // is (ALUOpType.sllw) { res := W(rs << rt(5, 0)) }
      // is (ALUOpType.srlw) { res := W(rs >> rt(5, 0)) }
      // is (ALUOpType.sraw) { res := W((rs.asSInt >> rt(5, 0)).asUInt) }

      // LAB2: LAB1 Wrong
      // sllw & srlw & sraw : rt(5, 0) -> rt(4, 0)
      // srlw & sraw        : rs -> rs(31, 0)
      is (ALUOpType.sllw) { res := W(rs << rt(4, 0)) }
      is (ALUOpType.srlw) { res := W(rs(31, 0) >> rt(4, 0)) }
      is (ALUOpType.sraw) { res := W((rs(31, 0).asSInt >> rt(4, 0)).asUInt) }
    }
  }
  io.result := res

}
