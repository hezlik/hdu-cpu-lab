// LAB3: MDU Module

package cpu.pipeline

import chisel3._
import chisel3.util._
import cpu.defines._
import cpu.defines.Const._

class Mdu extends Module {
  val io = IO(new Bundle {
    val info     = Input(new Info())
    val src_info = Input(new SrcInfo())
    val result   = Output(UInt(XLEN.W))
  })

  val valid = io.info.valid
  val op    = io.info.op
  val rs    = io.src_info.src1_data
  val rt    = io.src_info.src2_data

  def W(x : UInt) = {
    val x32 = x(31, 0)
    Cat(Fill(32, x32(31)), x32)
  }

  val res = Wire(UInt(XLEN.W))

  res := 0.U

  when (valid) {
    switch (op) {
      is (MDUOpType.   mul) { res := rs * rt }
      is (MDUOpType.  mulh) { res := (rs.asSInt * rt.asSInt)(127, 64) }
      is (MDUOpType.mulhsu) { res := (rs.asSInt * rt).asSInt(127, 64) }
      is (MDUOpType. mulhu) { res := (rs * rt)(127, 64) }
      is (MDUOpType.   div) {
        res := Mux(
          rt === 0.U,
          "hffffffffffffffff".U,
          (rs.asSInt / rt.asSInt)(63, 0)
        )
      }
      is (MDUOpType.  divu) {
        res := Mux(
          rt === 0.U,
          "hffffffffffffffff".U,
          rs / rt
        )
      }
      is (MDUOpType.   rem) {
        res := Mux(
          rt === 0.U,
          rs,
          (rs.asSInt - rt.asSInt * (rs.asSInt / rt.asSInt))(63, 0)
        )
      }
      is (MDUOpType.  remu) {
        res := Mux(
          rt === 0.U,
          rs,
          rs % rt
        )
      }
      is (MDUOpType.  mulw) { res := W(rs * rt) }
      is (MDUOpType.  divw) {
        res := Mux(
          rt(31,0) === 0.U,
          "hffffffffffffffff".U,
          W((rs(31, 0).asSInt / rt(31, 0).asSInt).asUInt)
        )
      }
      is (MDUOpType. divuw) {
        res := Mux(
          rt(31, 0) === 0.U,
          "hffffffffffffffff".U,
          W(rs(31, 0) / rt(31, 0))
        )
      }
      is (MDUOpType.  remw) {
        res := Mux(
          rt(31, 0) === 0.U,
          W(rs(31, 0)),
          W((rs(31, 0).asSInt % rt(31, 0).asSInt).asUInt)
        )
      }
      is (MDUOpType. remuw) {
        res := Mux(
          rt(31, 0) === 0.U,
          W(rs(31,0)),
          W(rs(31, 0) % rt(31, 0))
        )
      }
    }
  }
  
  io.result := res

}
