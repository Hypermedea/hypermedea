(define (problem pb)
  (:domain itm-factory)
  (:objects
    agv
    model0 model1 model2 model3 model4 model5 workstation0 workstation1 workstation2 workstation3 workstation4 item0 locstorage loccharging loc0 loc1 loc2 loc3 loc4 loc5 loc6 loc7 loc8)
  (:init
    (transportationdevice agv)
    (isat agv loccharging)
    (workstation workstation0) (off workstation0) (isat workstation0 loc0) (producesmodel workstation0 model0) (consumesmodel workstation0 model1) (workstation workstation1) (off workstation1) (isat workstation1 loc1) (producesmodel workstation1 model1) (consumesmodel workstation1 model2) (workstation workstation2) (off workstation2) (isat workstation2 loc2) (producesmodel workstation2 model2) (consumesmodel workstation2 model3) (workstation workstation3) (off workstation3) (isat workstation3 loc3) (producesmodel workstation3 model3) (consumesmodel workstation3 model4) (workstation workstation4) (off workstation4) (isat workstation4 loc4) (producesmodel workstation4 model4) (consumesmodel workstation4 model5) (item item0) (model item0 model5) (isat item0 locstorage) (haspathto locstorage loc0) (haspathto loccharging loc0) (haspathto loc0 loc3) (haspathto loc0 loc1) (haspathto loc0 locstorage) (haspathto loc0 loccharging) (haspathto loc1 loc4) (haspathto loc1 loc0) (haspathto loc1 loc2) (haspathto loc2 loc5) (haspathto loc2 loc1) (haspathto loc2 loc3) (haspathto loc3 loc6) (haspathto loc3 loc0) (haspathto loc3 loc2) (haspathto loc3 loc4) (haspathto loc4 loc7) (haspathto loc4 loc1) (haspathto loc4 loc3) (haspathto loc4 loc5) (haspathto loc5 loc8) (haspathto loc5 loc2) (haspathto loc5 loc4) (haspathto loc5 loc6) (haspathto loc6 loc3) (haspathto loc6 loc5) (haspathto loc6 loc7) (haspathto loc7 loc4) (haspathto loc7 loc6) (haspathto loc7 loc8) (haspathto loc8 loc5) (haspathto loc8 loc7))
  (:goal (or
    (and (model item0 model0) (isat item0 locstorage)))))