(define (problem pb)
  (:domain itm-factory)
  (:objects
    agv
    model0 model1 workstation0 item0 locstorage loccharging loc0)
  (:init
    (transportationdevice agv)
    (isat agv loccharging)
    (workstation workstation0) (off workstation0) (isat workstation0 loc0) (producesmodel workstation0 model0) (consumesmodel workstation0 model1) (item item0) (model item0 model1) (isat item0 locstorage) (haspathto locstorage loc0) (haspathto loccharging loc0) (haspathto loc0 locstorage) (haspathto loc0 loccharging))
  (:goal (or
    (and (model item0 model0) (isat item0 locstorage)))))