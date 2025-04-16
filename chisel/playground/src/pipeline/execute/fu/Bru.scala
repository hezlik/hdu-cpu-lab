// LAB5: BRU Module

package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class Bru extends Module {
  val io = IO(new Bundle {
    val info     = Input(new Info())
    val src_info = Input(new SrcInfo())
    val pc       = Input(UInt(XLEN.W))
    val ftcInfo  = Output(new FetchInfo())
    val result   = Output(UInt(XLEN.W))
  })
  
  val valid  = io.info.valid
  val op     = io.info.op
  val rs     = io.src_info.src1_data
  val rt     = io.src_info.src2_data
  val pc     = io.pc
  val imm    = io.info.imm
  val new_pc = pc + imm

  val branch = Wire(Bool())
  val target = Wire(UInt(XLEN.W))
  val res    = Wire(UInt(XLEN.W))

  branch := false.B
  target := 0.U
  res    := 0.U
  
  when (valid) {
    switch (op){
      is (BRUOpType.beq) {
        when (rs === rt) {
          branch := true.B
          target := new_pc
          // print("       114514        \n\n")
          // print(io.ftcInfo.branch)
          // print(io.ftcInfo.target)
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
  }

  io.ftcInfo.branch := branch
  io.ftcInfo.target := target
  io.result         := res

}
