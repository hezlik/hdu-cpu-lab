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

// 功能单元类型 Function Unit Type
// object FuType {
//   def num     = 1
//   def alu     = 0.U // arithmetic logic unit
//   def apply() = UInt(log2Up(num).W)
// }

// LAB3: FuType
// object FuType {
//   def num     = 2
//   def alu     = 0.U
//   def mdu     = 1.U
//   def apply() = UInt(log2Up(num).W)
// }

// LAB4: FuType
// object FuType {
//   def num     = 3
//   def alu     = 0.U
//   def mdu     = 1.U
//   def lsu     = 2.U
//   def apply() = UInt(log2Up(num).W)
// }

// LAB5: FuType
// object FuType {
//   def num     = 4
//   def alu     = 0.U
//   def mdu     = 1.U
//   def lsu     = 2.U
//   def bru     = 3.U
//   def apply() = UInt(log2Up(num).W)
// }

// LAB8: FuType
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

  // LAB9: ALU New Instruction : nop
  def nop  = 15.U

}

// LAB3: MDUOpType
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

// LAB4: LSUOpType
// object LSUOpType {

//   def lb  = 0.U
//   def lh  = 1.U
//   def lw  = 2.U
//   def ld  = 3.U
//   def lbu = 4.U
//   def lhu = 5.U
//   def lwu = 6.U
//   def sb  = 7.U
//   def sh  = 8.U
//   def sw  = 9.U
//   def sd  = 10.U

//   def isLoad(op : UInt) = {op < 7.U}
//   def isStore(op : UInt) = { op >= 7.U }

// }

// LAB9: LSUOpType
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

// LAB5: BRUOpType
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

// LAB8: CSROpType
object CSROpType {

  def csrrw  = 0.U
  def csrrs  = 1.U
  def csrrc  = 2.U

  def isCSRI(inst : UInt) : Bool = inst(14)

  // LAB9: New CSR instructions : ecall, ebreak, mret
  def ecall  = 3.U
  def ebreak = 4.U
  def mret   = 5.U

}

// LAB9: CSRAddr
object CSRAddr {
  def empty_mask      = "b0000_0000_0000"
  def cycle_mask      = "b1100_0000_0000"
  def mvendorid_mask  = "b1111_0001_0001"
  def marchid_mask    = "b1111_0001_0010"
  def mimpid_mask     = "b1111_0001_0011"
  def mhartid_mask    = "b1111_0001_0100"
  def mstatus_mask    = "b0011_0000_0000"
  def misa_mask       = "b0011_0000_0001"
  def mie_mask        = "b0011_0000_0100"
  def mtvec_mask      = "b0011_0000_0101"
  def mcounteren_mask = "b0011_0000_0110"
  def mscratch_mask   = "b0011_0100_0000"
  def mepc_mask       = "b0011_0100_0001"
  def mcause_mask     = "b0011_0100_0010"
  def mtval_mask      = "b0011_0100_0011"
  def mip_mask        = "b0011_0100_0100"

  def satp_mask       = "b0001_0100_0000"
  def medeleg_mask    = "b0011_0000_0010"
  def mideleg_mask    = "b0011_0000_0011"
  def pmpcfg0_mask    = "b0011_0110_0000"
  def pmpaddr0_mask   = "b0011_0111_0000"
  def tselect_mask    = "b0111_1010_0000" 
  def tdata1_mask     = "b0111_1010_0001" 
  def tdata2_mask     = "b0111_1010_0010" 

  def empty      = empty_mask.U
  def cycle      = cycle_mask.U
  def mvendorid  = mvendorid_mask.U
  def marchid    = marchid_mask.U
  def mimpid     = mimpid_mask.U
  def mhartid    = mhartid_mask.U
  def mstatus    = mstatus_mask.U
  def misa       = misa_mask.U
  def mie        = mie_mask.U
  def mtvec      = mtvec_mask.U
  def mcounteren = mcounteren_mask.U
  def mscratch   = mscratch_mask.U
  def mepc       = mepc_mask.U
  def mcause     = mcause_mask.U
  def mtval      = mtval_mask.U
  def mip        = mip_mask.U

  def satp       = satp_mask.U
  def medeleg    = medeleg_mask.U
  def mideleg    = mideleg_mask.U
  def pmpcfg0    = pmpcfg0_mask.U
  def pmpaddr0   = pmpaddr0_mask.U
  def tselect    = tselect_mask.U
  def tdata1     = tdata1_mask.U
  def tdata2     = tdata2_mask.U

  def apply() = UInt(VT_CSR_ADDR_WID.W)

  def leastMode(addr : UInt) = addr(9, 8)
  def enRead(addr : UInt) = true.B
  def enWrite(addr : UInt) = addr(11, 10) =/= "b11".U
}

// LAB9: Privilege
object Privilege {
  val u: UInt = "b00".U  // 用户模式
  val s: UInt = "b01".U  // 监管模式
  val h: UInt = "b10".U  // 虚拟机监管模式
  val m: UInt = "b11".U  // 机器模式
  
  def apply() = UInt(MODE_WID.W)
}