package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._
import cpu.CpuConfig

class Fu extends Module {
  val io = IO(new Bundle {
    val data = new Bundle {
      val pc       = Input(UInt(XLEN.W))
      val info     = Input(new Info())
      val src_info = Input(new SrcInfo())
      val rd_info  = Output(new RdInfo())
    }

    val dataSram = new DataSram()
  })

  // val alu = Module(new Alu()).io

  io.dataSram.en    := false.B
  io.dataSram.addr  := DontCare
  io.dataSram.wdata := DontCare
  io.dataSram.wen   := 0.U

  // alu.info     := io.data.info
  // alu.src_info := io.data.src_info

  // io.data.rd_info.wdata := alu.result

  // LAB3: Reconstruct Logic of FU
  val res = Wire(UInt(XLEN.W))
  res := 0.U
  switch (io.data.info.fusel) {
    is (FuType.alu) {
      val alu = Module(new Alu()).io
      alu.info     := io.data.info
      alu.src_info := io.data.src_info
      res          := alu.result
    }
    is (FuType.mdu) {
      val mdu = Module(new Mdu()).io
      mdu.info     := io.data.info
      mdu.src_info := io.data.src_info
      res          := mdu.result
    }
    // LAB4: New FU : LSU
    is (FuType.lsu) {
      val lsu = Module(new Lsu()).io
      lsu.info     := io.data.info
      lsu.src_info := io.data.src_info
      io.dataSram  <> lsu.dataSram 
    }
  }
  io.data.rd_info.wdata := res

}
