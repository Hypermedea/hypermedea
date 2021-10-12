(define (problem pb)
  (:domain itm-factory)
  (:objects
    agv
    model0 model1 model2 workstation0 workstation1 item0 locstorage loccharging loc0 loc1 loc2 loc3)
  (:init
    (transportationdevice agv)
    (isat agv loccharging)
    (workstation workstation0) (off workstation0) (isat workstation0 loc0) (producesmodel workstation0 model0) (consumesmodel workstation0 model1) (workstation workstation1) (off workstation1) (isat workstation1 loc1) (producesmodel workstation1 model1) (consumesmodel workstation1 model2) (item item0) (model item0 model2) (isat item0 locstorage) (haspathto locstorage loc0) (haspathto loccharging loc0) (haspathto loc0 loc2) (haspathto loc0 loc1) (haspathto loc0 locstorage) (haspathto loc0 loccharging) (haspathto loc1 loc3) (haspathto loc1 loc0) (haspathto loc1 loc2) (haspathto loc2 loc0) (haspathto loc2 loc1) (haspathto loc2 loc3) (haspathto loc3 loc1) (haspathto loc3 loc2))
  (:goal (or
    (and (model item0 model0) (isat item0 locstorage)))))