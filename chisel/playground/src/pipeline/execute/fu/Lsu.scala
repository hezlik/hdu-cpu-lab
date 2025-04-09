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

  io.dataSram.en    := !reset.asBool
  io.dataSram.wen   := 0.U
  io.dataSram.addr  := io.src_info.src1_data + io.info.imm
  io.dataSram.wdata := 0.U

  when (io.info.valid && io.info.src2_ren) {
    val rt   = io.src_info.src2_data
    val addr = io.dataSram.addr
    switch (io.info.op) {
      is (LSUOpType.sb) {
        io.dataSram.wen   := "b0000_0001".U << addr(2, 0)
        io.dataSram.wdata := Fill(8, rt(7, 0))
      }
      is (LSUOpType.sh) {
        io.dataSram.wen   := "b0000_0011".U << addr(2, 0)
        io.dataSram.wdata := Fill(4, rt(15, 0))
      }
      is (LSUOpType.sw) {
        io.dataSram.wen   := "b0000_1111".U << addr(2, 0)
        io.dataSram.wdata := Fill(2, rt(31, 0))
      }
      is (LSUOpType.sd) {
        io.dataSram.wen   := "b1111_1111".U << addr(2, 0)
        io.dataSram.wdata := Fill(1, rt(63, 0))
      }
    }
  }

}
