package cpu.defines

import chisel3._
import chisel3.util._
import cpu.CpuConfig

trait CoreParameter {
  def cpuConfig = new CpuConfig
  val XLEN      = if (cpuConfig.isRV32) 32 else 64
  val VADDR_WID = if (cpuConfig.isRV32) 32 else 39
  val PADDR_WID = 32
}

trait Constants extends CoreParameter {
  // 全局
  val PC_INIT = "h80000000".U(XLEN.W)

  val INT_WID = 12
  val EXC_WID = 16

  // inst rom
  val INST_WID = 32

  // GPR RegFile
  val AREG_NUM     = 32
  val REG_ADDR_WID = 5

  // LAB8: CSR RegFile
  // val CREG_NUM        = 16
  // val CSR_ADDR_WID    = 4
  val VT_CSR_ADDR_WID = 12

  // LAB9: CSR RegFile
  val CREG_NUM        = 32
  val CSR_ADDR_WID    = 5

  // LAB9: CSR Mode
  val MODE_WID        = 2

}

// LAB9: HasExceptionNO
trait HasExceptionNO extends Constants {
  val instAddrMisaligned  = 0
  val instAccessFault     = 1
  val illegalInst         = 2
  val breakPoint          = 3
  val loadAddrMisaligned  = 4
  val loadAccessFault     = 5
  val storeAddrMisaligned = 6
  val storeAccessFault    = 7
  val ecallU              = 8
  val ecallS              = 9
  val ecallM              = 11
  val instPageFault       = 12
  val loadPageFault       = 13
  val storePageFault      = 15

  val noException         = 14

  val Exceptionity = Seq(
    breakPoint,
    instPageFault,
    instAccessFault,
    illegalInst,
    instAddrMisaligned,
    ecallM,
    ecallS,
    ecallU,
    storeAddrMisaligned,
    loadAddrMisaligned,
    storePageFault,
    loadPageFault,
    storeAccessFault,
    loadAccessFault,
  )
}

// LAB9: HasInterruptNO
trait HasInterruptNO extends Constants {
  val svSoftwareInterrupt  = 1
  val macSoftwareInterrupt = 3
  val svTimerInterrupt     = 5
  val macTimerInterrupt    = 7
  val svExternalInterrupt  = 9
  val macExternalInterrupt = 11

  val noInterrupt           = 10

  val Interruptionity = Seq(
    macExternalInterrupt,
    macSoftwareInterrupt,
    macTimerInterrupt,
    svExternalInterrupt,
    svSoftwareInterrupt,
    svTimerInterrupt,
  )
}

trait SRAMConst extends Constants {
  val SRAM_ADDR_WID      = PADDR_WID // 32
  val DATA_SRAM_DATA_WID = XLEN
  val DATA_SRAM_WEN_WID  = XLEN / 8
  val INST_SRAM_DATA_WID = INST_WID
  val INST_SRAM_WEN_WID  = INST_WID / 8
}
// object Const extends Constants with SRAMConst

// LAB9: New Const : HasExceptionNO & HasInterruptNO
object Const extends Constants with SRAMConst with HasExceptionNO with HasInterruptNO

object Instructions extends HasInstrType with CoreParameter {
  def NOP           = 0x00000013.U
  // val DecodeDefault = List(InstrN, FuType.alu, ALUOpType.add)
  // LAB9: Undecodable instruction : add -> nop
  val DecodeDefault = List(InstrN, FuType.alu, ALUOpType.nop)
  def DecodeTable   = RVIInstr.table
}