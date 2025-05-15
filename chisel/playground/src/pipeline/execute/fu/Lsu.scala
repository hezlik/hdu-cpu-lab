package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class Lsu extends Module {
  val io = IO(new Bundle {
    val info     = Input(new Info())
    val src_info = Input(new SrcInfo())

    // Read & Write DataSRAM
    val dataSram = new DataSram()

    // Exceptions & Interruptions
    val old_ex    = Input(new ExceptionInfo())
    val new_ex    = Output(new ExceptionInfo())
  })

  io.new_ex := io.old_ex

  val valid = io.info.valid
  val op    = io.info.op
  val rt    = io.src_info.src2_data
  val addr  = io.src_info.src1_data + io.info.imm

  val wen   = WireInit(0.U(DATA_SRAM_WEN_WID.W))
  val wdata = WireInit(0.U(DATA_SRAM_DATA_WID.W))

  when (valid) {
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

    // Exception : storeAddrMisaligned & loadAddrMisaligned
    val bit = WireInit(0.U(SRAM_ADDR_WID.W))
    
    switch (LSUOpType.exBit(op)) {
      is (LSUOpType.b) { bit := 0.U }
      is (LSUOpType.h) { bit := addr(0) }
      is (LSUOpType.w) { bit := addr(1, 0) }
      is (LSUOpType.d) { bit := addr(2, 0) }
    }

    when (bit =/= 0.U) {
      wen := false.B
      when (LSUOpType.isStore(op)) {
        io.new_ex.exception(storeAddrMisaligned) := true.B
        io.new_ex.tval(storeAddrMisaligned)      := addr
      }.otherwise {
        io.new_ex.exception(loadAddrMisaligned) := true.B
        io.new_ex.tval(loadAddrMisaligned)      := addr
      }
    }
  }

  io.dataSram.en    := !reset.asBool
  io.dataSram.wen   := wen
  io.dataSram.addr  := addr
  io.dataSram.wdata := wdata
}
