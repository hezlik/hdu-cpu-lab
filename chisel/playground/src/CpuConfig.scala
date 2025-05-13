package cpu

import chisel3.util._
import cpu.defines.Const._

case class CpuConfig(
  // 指令集
  val isRV32:               Boolean = false, // 是否为RV32
  // val hasMExtension:        Boolean = false, // 是否实现M扩展，即乘除法指令
  // LAB3: RV64M Open
  val hasMExtension:        Boolean = true,
  // val hasZicsrExtension:    Boolean = false, // 是否实现Zicsr扩展，即CSR指令
  // LAB8: CSR Open
  val hasZicsrExtension:    Boolean = true,
  val hasZifenceiExtension: Boolean = false, // 是否实现Zifencei扩展，即FENCE.I指令
  val hasAExtension:        Boolean = false, // 是否实现A扩展，即原子指令
  // 特权模式
  val hasSMode: Boolean = false, // 是否有S模式
  // val hasUMode: Boolean = false // 是否有U模式
  // LAB9: U mode Open
  val hasUMode: Boolean = true
)
