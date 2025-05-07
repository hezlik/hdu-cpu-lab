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
    // LAB5: Fu New Output : ftcInfo
    val ftcInfo  = Output(new FetchInfo())
    // LAB8: Fu New Interaction : csr_read & csr_write
    val csr_read  = new CsrRead()
    val csr_write = new CsrWrite()
  })

  // val alu = Module(new Alu()).io

  io.dataSram.en     := false.B
  io.dataSram.addr   := DontCare
  io.dataSram.wdata  := DontCare
  io.dataSram.wen    := 0.U

  // LAB5: Initialize ftcInfo
  // io.ftcInfo.branch  := 0.U
  io.ftcInfo.target  := 0.U

  // LAB9: Rename Fetch Info : branch -> flush
  io.ftcInfo.flush   := 0.U

  // LAB8: Initialize csr_read & csr_write
  io.csr_read.raddr  := 0.U
  io.csr_write.wen   := false.B
  io.csr_write.waddr := 0.U
  io.csr_write.wdata := 0.U

  // alu.info     := io.data.info
  // alu.src_info := io.data.src_info

  // io.data.rd_info.wdata := alu.result

  // LAB3: Reconstruct Logic of FU
  val res = Wire(UInt(XLEN.W))

  res := 0.U
  
  switch (io.data.info.fusel) {
    is (FuType.alu) {
      val alu = Module(new Alu()).io
      alu.info      := io.data.info
      alu.src_info  := io.data.src_info
      res           := alu.result
    }
    is (FuType.mdu) {
      val mdu = Module(new Mdu()).io
      mdu.info      := io.data.info
      mdu.src_info  := io.data.src_info
      res           := mdu.result
    }
    // LAB4: New FU : LSU
    is (FuType.lsu) {
      val lsu = Module(new Lsu()).io
      lsu.info      := io.data.info
      lsu.src_info  := io.data.src_info
      lsu.dataSram  <> io.dataSram
    }
    // LAB5: New FU : BRU
    is (FuType.bru) {
      val bru = Module(new Bru()).io
      bru.info      := io.data.info
      bru.src_info  := io.data.src_info
      bru.pc        := io.data.pc
      bru.ftcInfo   <> io.ftcInfo
      res           := bru.result
    }
    // LAB8: New FU : CSR
    is (FuType.csr) {
      val csr = Module(new Csr()).io
      csr.info      := io.data.info
      csr.src_info  := io.data.src_info
      csr.csr_read  <> io.csr_read
      csr.csr_write <> io.csr_write
      res           := csr.result
    }
  }

  io.data.rd_info.wdata := res

}
