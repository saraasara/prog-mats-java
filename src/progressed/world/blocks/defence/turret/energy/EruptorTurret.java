package progressed.world.blocks.defence.turret.energy;

import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import progressed.content.effects.*;
import progressed.content.effects.UtilFx.*;
import progressed.graphics.*;
import progressed.util.*;
import progressed.world.meta.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class EruptorTurret extends PowerTurret{
    public final int beamTimer = timers++;
    public float beamInterval = 2f, beamStroke = 3f, beamWidth = 16f;
    public Color beamColor = PMPal.magma;
    public Effect beamEffect = EnergyFx.eruptorBurn;

    public float shootDuration = 60f;

    public EruptorTurret(String name){
        super(name);

        targetAir = false;
        shootSound = Sounds.none;
        loopSound = Sounds.beam;
        loopSoundVolume = 2f;
        heatColor = Color.valueOf("f08913");
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(Stat.ammo);
        stats.add(Stat.ammo, PMStatValues.ammo(ObjectMap.of(this, shootType)));
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("pm-reload", (EruptorTurretBuild entity) -> new Bar(
            () -> bundle.format("bar.pm-reload", PMUtls.stringsFixed(Mathf.clamp(entity.reload / reloadTime) * 100f)),
            () -> entity.team.color,
            () -> Mathf.clamp(entity.reload / reloadTime)
        ));
    }

    public class EruptorTurretBuild extends PowerTurretBuild{
        protected Bullet bullet;
        protected float bulletLife, lengthScl;

        @Override
        public void targetPosition(Posc pos){
            if(!hasAmmo() || pos == null) return;
            var offset = Tmp.v1.setZero();

            //when delay is accurate, assume unit has moved by chargeTime already
            if(accurateDelay && pos instanceof Hitboxc h){
                offset.set(h.deltaX(), h.deltaY()).scl(chargeTime / Time.delta);
            }

            targetPos.set(Predict.intercept(this, pos, offset.x, offset.y, range / shootDuration));

            if(targetPos.isZero()){
                targetPos.set(pos);
            }
        }

        @Override
        public void updateTile(){
            super.updateTile();

            if(bulletLife > 0 && bullet != null){
                wasShooting = true;
                tr.trns(rotation, lengthScl * range, 0f);
                bullet.set(x + tr.x, y + tr.y);
                bullet.time(0f);
                recoil = recoilAmount;
                heat = 1f;
                bulletLife -= Time.delta / Math.max(efficiency(), 0.00001f);
                lengthScl += Time.delta / shootDuration;
                if(timer(beamTimer, beamInterval)){
                    tr2.trns(rotation, shootLength - recoil);
                    UtilFx.lightning.at(x + tr2.x, y + tr2.y, angleTo(bullet), beamColor, new LightningData(bullet, beamStroke, true, beamWidth));
                    beamEffect.at(bullet, rotation);
                }
                if(bulletLife <= 0f){
                    bullet = null;
                    lengthScl = 0f;
                }
            }else if(reload < reloadTime){
                float maxUsed = consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount;
                Liquid liquid = liquids.current();

                float used = Math.min(liquids.get(liquid), maxUsed * Time.delta) * baseReloadSpeed();
                reload += used * liquid.heatCapacity * coolantMultiplier;
                liquids.remove(liquid, used);

                if(Mathf.chance(0.06 * used)){
                    coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
                }
            }
        }

        @Override
        public boolean shouldTurn(){
            return lengthScl < 0.001f;
        }

        @Override
        protected void updateCooling(){
            //Copied into updateTile so that it isn't always running.
        }
        @Override
        protected void updateShooting(){
            if(bulletLife > 0 && bullet != null){
                return;
            }

            super.updateShooting();
        }
        
        @Override
        protected void bullet(BulletType type, float angle){
            bullet = type.create(this, team, x + tr.x, y + tr.y, angle);
            lengthScl = 0;
            bulletLife = shootDuration;
        }

        @Override
        public boolean shouldActiveSound(){
            return bulletLife > 0 && bullet != null;
        }
    }
}
