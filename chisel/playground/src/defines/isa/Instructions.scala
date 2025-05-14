package cpu.defines

import chisel3._
import chisel3.util._

// Instructions : New import : Const
import cpu.defines._
import cpu.defines.Const._

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

// FuType : alu, mdu, lsu, bru, csr
object FuType {
  def num     = 5
  def alu     = 0.U
  def mdu     = 1.U
  def lsu     = 2.U
  def bru     = 3.U
  def csr     = 4.U
  def apply() = UInt(log2Up(num).W)
}

// 功能单元操作类型 Function Unit Operation Type
object FuOpType {
  def apply() = UInt(5.W) // 宽度与最大的功能单元操作类型宽度一致
}

// 算术逻辑单元操作类型 Arithmetic Logic Unit Operation Type
object ALUOpType {
  def nop  = 0.U
  def add  = 1.U
  def sub  = 2.U
  def sll  = 3.U
  def slt  = 4.U
  def sltu = 5.U
  def xor  = 6.U
  def srl  = 7.U
  def sra  = 8.U
  def or   = 9.U
  def and  = 10.U
  def addw = 11.U
  def subw = 12.U
  def sllw = 13.U
  def srlw = 14.U
  def sraw = 15.U
}

object MDUOpType {
  def mul    = 0.U
  def mulh   = 1.U
  def mulhsu = 2.U
  def mulhu  = 3.U
  def div    = 4.U
  def divu   = 5.U
  def rem    = 6.U
  def remu   = 7.U
  def mulw   = 8.U
  def divw   = 9.U
  def divuw  = 10.U
  def remw   = 12.U
  def remuw  = 13.U
}

object LSUOpType {
  def lb  = "b0000".U
  def lh  = "b0001".U
  def lw  = "b0010".U
  def ld  = "b0011".U
  def lbu = "b0100".U
  def lhu = "b0101".U
  def lwu = "b0110".U
  def sb  = "b1000".U
  def sh  = "b1001".U
  def sw  = "b1010".U
  def sd  = "b1011".U

  def b   = "b00".U
  def h   = "b01".U
  def w   = "b10".U
  def d   = "b11".U

  def isLoad(op : UInt) : Bool = !op(3)
  def isStore(op : UInt) : Bool = op(3)
  def exBit(op : UInt) = op(1, 0)
}

object BRUOpType {
  def beq  = 0.U
  def bne  = 1.U
  def blt  = 2.U
  def bge  = 3.U
  def bltu = 4.U
  def bgeu = 5.U
  def jal  = 6.U
  def jalr = 7.U
}

object CSROpType {
  def csrrw  = 0.U
  def csrrs  = 1.U
  def csrrc  = 2.U

  def ecall  = 3.U
  def ebreak = 4.U
  def mret   = 5.U

  def isCSRI(inst : UInt) : Bool = inst(14)
}

object CSRAddr {
  def empty      = "b0000_0000_0000"

  def cycle      = "b1100_0000_0000"

  def mvendorid  = "b1111_0001_0001"
  def marchid    = "b1111_0001_0010"
  def mimpid     = "b1111_0001_0011"
  def mhartid    = "b1111_0001_0100"

  def mstatus    = "b0011_0000_0000"
  def medeleg    = "b0011_0000_0010"
  def mideleg    = "b0011_0000_0011"
  def misa       = "b0011_0000_0001"
  def mie        = "b0011_0000_0100"
  def mtvec      = "b0011_0000_0101"
  def mcounteren = "b0011_0000_0110"

  def mscratch   = "b0011_0100_0000"
  def mepc       = "b0011_0100_0001"
  def mcause     = "b0011_0100_0010"
  def mtval      = "b0011_0100_0011"
  def mip        = "b0011_0100_0100"
  
  def satp       = "b0001_0100_0000"

  def pmpcfg0    = "b0011_0110_0000"
  def pmpaddr0   = "b0011_0111_0000"

  def tselect    = "b0111_1010_0000"
  def tdata1     = "b0111_1010_0001"
  def tdata2     = "b0111_1010_0010"

  def apply() = UInt(VT_CSR_ADDR_WID.W)

  def leastMode(addr : UInt) = addr(9, 8)
  def enRead(addr : UInt) = true.B
  def enWrite(addr : UInt) = addr(11, 10) =/= "b11".U
}

object Privilege {
  val u: UInt = "b00".U  // 用户模式
  val s: UInt = "b01".U  // 监管模式
  val h: UInt = "b10".U  // 虚拟机监管模式
  val m: UInt = "b11".U  // 机器模式
  
  def apply() = UInt(MODE_WID.W)
}