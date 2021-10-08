(define (problem pb)
  (:domain itm-factory)
  (:objects
    agv
    model0 model1 model2 model3 model4 workstation0 workstation1 workstation2 workstation3 item0 locstorage loccharging loc0 loc1 loc2 loc3)
  (:init
    (transportationdevice agv)
    (isat agv loccharging)
    (workstation workstation0) (off workstation0) (isat workstation0 loc0) (producesmodel workstation0 model0) (consumesmodel workstation0 model1) (workstation workstation1) (off workstation1) (isat workstation1 loc1) (producesmodel workstation1 model1) (consumesmodel workstation1 model2) (workstation workstation2) (off workstation2) (isat workstation2 loc2) (producesmodel workstation2 model2) (consumesmodel workstation2 model3) (workstation workstation3) (off workstation3) (isat workstation3 loc3) (producesmodel workstation3 model3) (consumesmodel workstation3 model4) (item item0) (model item0 model4) (isat item0 locstorage) (haspathto locstorage loc0) (haspathto loccharging loc0) (haspathto loc0 loc2) (haspathto loc0 locstorage) (haspathto loc0 loc1) (haspathto loc0 loccharging) (haspathto loc1 loc3) (haspathto loc1 loc0) (haspathto loc2 loc0) (haspathto loc2 loc3) (haspathto loc3 loc1) (haspathto loc3 loc2))
  (:goal (or
    (and (model item0 model0) (isat item0 locstorage)))))