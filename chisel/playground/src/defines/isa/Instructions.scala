package cpu.defines

import chisel3._
import chisel3.util._

// 指令类型
trait HasInstrType {
  def InstrN = "b000".U
  def InstrI = "b100".U
  def InstrR = "b101".U
  def InstrS = "b010".U
  def InstrB = "b001".U
  def InstrU = "b110".U
  def InstrJ = "b111".U

  // I、R、U、J类型的指令都需要写寄存器
  def isRegWen(instrType: UInt): Bool = instrType(2)
}

// 功能单元类型 Function Unit Type
object FuType {
  def num     = 1
  def alu     = 0.U // arithmetic logic unit
  def apply() = UInt(log2Up(num).W)
}

// 功能单元操作类型 Function Unit Operation Type
object FuOpType {
  def apply() = UInt(5.W) // 宽度与最大的功能单元操作类型宽度一致
}

// 算术逻辑单元操作类型 Arithmetic Logic Unit Operation Type
object ALUOpType {
  // def add = 1.U
  // TODO: 定义更多的ALU操作类型

  // LAB1: ALUOpType
  def add  = 0.U
  def sub  = 1.U
  def sll  = 2.U
  def slt  = 3.U
  def sltu = 4.U
  def xor  = 5.U
  def srl  = 6.U
  def sra  = 7.U
  def or   = 8.U
  def and  = 9.U
  def addw = 10.U
  def subw = 11.U
  def sllw = 12.U
  def srlw = 13.U
  def sraw = 14.U

}
