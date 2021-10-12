(define (problem pb)
  (:domain itm-factory)
  (:objects
    agv
    model0 item0 locstorage loccharging)
  (:init
    (transportationdevice agv)
    (isat agv loccharging)
    (item item0) (model item0 model0) (isat item0 locstorage) (haspathto locstorage loc0) (haspathto loccharging loc0))
  (:goal (or
    (and (model item0 model0) (isat item0 locstorage)))))