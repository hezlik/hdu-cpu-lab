package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class Bru extends Module {
  val io = IO(new Bundle {
    val info      = Input(new Info())
    val src_info  = Input(new SrcInfo())
    val pc        = Input(UInt(XLEN.W))
    val result    = Output(UInt(XLEN.W))

    // Control Conflict : Commit ftc_info
    val ftc_info  = Output(new FetchInfo())

    // Exceptions & Interruptions
    val old_ex    = Input(new ExceptionInfo())
    val new_ex    = Output(new ExceptionInfo())
  })

  io.new_ex := io.old_ex
  
  val valid  = io.info.valid
  val op     = io.info.op
  val rs     = io.src_info.src1_data
  val rt     = io.src_info.src2_data
  val pc     = io.pc
  val imm    = io.info.imm
  val new_pc = pc + imm

  val branch = WireInit(false.B)
  val target = WireInit(0.U(XLEN.W))
  val res    = WireInit(0.U(XLEN.W))
  
  when (valid) {
    switch (op){
      is (BRUOpType.beq) {
        when (rs === rt) {
          branch := true.B
          target := new_pc
        }
      }
      is (BRUOpType.bne) {
        when (rs =/= rt) {
          branch := true.B
          target := new_pc
        }
      }
      is (BRUOpType.blt) {
        when (rs.asSInt < rt.asSInt) {
          branch := true.B
          target := new_pc
        }
      }
      is (BRUOpType.bge) {
        when (rs.asSInt >= rt.asSInt) {
          branch := true.B
          target := new_pc
        }
      }
      is (BRUOpType.bltu) {
        when (rs < rt) {
          branch := true.B
          target := new_pc
        }
      }
      is (BRUOpType.bgeu) {
        when (rs >= rt) {
          branch := true.B
          target := new_pc
        }
      }
      is (BRUOpType.jal) {
        branch := true.B
        target := new_pc
        res    := pc + 4.U
      }
      is (BRUOpType.jalr) {
        branch := true.B
        target := (rs + imm) & Cat(Fill(63,"b1".U),"b0".U)
        res    := pc + 4.U
      }
    }

    // Exception : instAddrMisaligned
    when (branch && target(1, 0) =/= "b00".U) {
      io.new_ex.exception(instAddrMisaligned) := true.B
      io.new_ex.tval(instAddrMisaligned)      := target
    }
  }

  io.ftc_info.flush  := branch
  io.ftc_info.target := target
  io.result          := res
}
