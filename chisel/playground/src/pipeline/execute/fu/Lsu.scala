// LAB4: LSU Module in Execute for Storage

package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class Lsu extends Module {
  val io = IO(new Bundle {
    val info     = Input(new Info())
    val src_info = Input(new SrcInfo())
    val dataSram = new DataSram()
  })

  val valid = io.info.valid
  val op    = io.info.op
  val rt    = io.src_info.src2_data
  val is_s  = io.info.src2_ren
  val addr  = io.src_info.src1_data + io.info.imm

  val wen   = Wire(UInt(DATA_SRAM_WEN_WID.W))
  val wdata = Wire(UInt(DATA_SRAM_DATA_WID.W))

  wen   := 0.U
  wdata := 0.U

  when (valid && is_s) {
    switch (op) {
      is (LSUOpType.sb) {
        wen   := "b0000_0001".U << addr(2, 0)
        wdata := Fill(8, rt(7, 0))
      }
      is (LSUOpType.sh) {
        wen   := "b0000_0011".U << addr(2, 0)
        wdata := Fill(4, rt(15, 0))
      }
      is (LSUOpType.sw) {
        wen   := "b0000_1111".U << addr(2, 0)
        wdata := Fill(2, rt(31, 0))
      }
      is (LSUOpType.sd) {
        wen   := "b1111_1111".U << addr(2, 0)
        wdata := Fill(1, rt(63, 0))
      }
    }
  }

  io.dataSram.en    := !reset.asBool
  io.dataSram.wen   := wen
  io.dataSram.addr  := addr
  io.dataSram.wdata := wdata

}
