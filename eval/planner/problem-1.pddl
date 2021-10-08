(define (problem pb)
  (:domain itm-factory)
  (:objects
    agv
    model0 item0 locstorage loccharging)
  (:init
    (transportationdevice agv)
    (isat agv loccharging)
    (item item0) (model item0 model0) (isat item0 locstorage)  )
  (:goal (or
    (and (model item0 model0) (isat item0 locstorage)))))