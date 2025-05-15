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

    // Read & Write DataSRAM
    val dataSram  = new DataSram()

    // Control Conflict : Commit ftc_info
    val ftc_info  = Output(new FetchInfo())

    // Read & Write CSRRegFile
    val csr_read  = new CsrRead()
    val csr_write = new CsrWrite()
    val mode      = Input(UInt(MODE_WID.W))

    // Exceptions & Interruptions
    val old_ex    = Input(new ExceptionInfo())
    val new_ex    = Output(new ExceptionInfo())
    val mret      = Output(Bool())
  })

  io.dataSram.en     := false.B
  io.dataSram.addr   := DontCare
  io.dataSram.wdata  := DontCare
  io.dataSram.wen    := 0.U

  io.ftc_info.target := 0.U
  io.ftc_info.flush  := 0.U

  io.csr_read.ren    := false.B
  io.csr_read.raddr  := 0.U
  io.csr_write.wen   := false.B
  io.csr_write.waddr := 0.U
  io.csr_write.wdata := 0.U

  io.new_ex          := io.old_ex
  io.mret            := false.B

  val res = WireInit(0.U(XLEN.W))
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
    is (FuType.lsu) {
      val lsu = Module(new Lsu()).io
      lsu.info      := io.data.info
      lsu.src_info  := io.data.src_info
      lsu.dataSram  <> io.dataSram
      lsu.old_ex    <> io.old_ex
      lsu.new_ex    <> io.new_ex
    }
    is (FuType.bru) {
      val bru = Module(new Bru()).io
      bru.info      := io.data.info
      bru.src_info  := io.data.src_info
      bru.pc        := io.data.pc
      bru.old_ex    <> io.old_ex
      bru.new_ex    <> io.new_ex
      bru.ftc_info  <> io.ftc_info
      res           := bru.result
    }
    is (FuType.csr) {
      val csr = Module(new Csr()).io
      csr.info      := io.data.info
      csr.src_info  := io.data.src_info
      csr.csr_read  <> io.csr_read
      csr.csr_write <> io.csr_write
      csr.mode      <> io.mode
      csr.old_ex    <> io.old_ex
      csr.new_ex    <> io.new_ex
      csr.mret      <> io.mret
      res           := csr.result
    }
  }

  io.data.rd_info.wdata := res
}
