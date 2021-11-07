package progressed.entities.bullet;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import progressed.content.*;
import progressed.content.PMFx.*;
import progressed.entities.*;
import progressed.world.blocks.defence.*;

public class PillarFieldBulletType extends BulletType{
    public IgneousPillar pillar;
    public float radius = 7.5f * 8f;
    public int amount = 1;

    public float fadeTime = 8f;
    public float ringStroke = 2f;
    public Color ringColor = Pal.darkerGray;
    public Effect ringEffect = PMFx.earthquke;
    public float ringEffectInterval = 2f;

    public int crackEffects = 1;
    public float crackStroke = 1.5f;
    public Color crackColor = Pal.darkerGray;
    public Effect crackEffect = PMFx.groundCrack;

    public Effect placeEffect = PMFx.pillarPlace;

    public PillarFieldBulletType(){
        super();

        absorbable = hittable = false;
        speed = 0;
        collides = false;
        hitEffect = despawnEffect = shootEffect = smokeEffect = Fx.none;
        layer = Layer.debris;
    }

    @Override
    public void init(){
        super.init();

        drawSize = Math.max(radius + ringStroke + 8f, drawSize);
    }

    @Override
    public void init(Bullet b){
        super.init(b);

        PillarFieldData data = new PillarFieldData();
        PMDamage.trueEachTile(b.x, b.y, radius, t -> data.tiles.add(t));
        data.tiles.shuffle();
        b.data = data;
    }

    @Override
    public void update(Bullet b){
        super.update(b);

        if(b.data instanceof PillarFieldData data && b.timer(1, lifetime / amount)){
            for(int i = 0; i < data.tiles.size; i++){
                Tile t = data.tiles.get(i);
                if(t.block() == Blocks.air){
                    boolean occupied = Groups.unit.intersect(t.worldx(), t.worldy(), 1, 1).contains(Flyingc::isGrounded);
                    if(!occupied){
                        data.tiles.remove(t);
                        placeEffect.at(t.worldx(), t.worldy(), pillar.size);
                        t.setNet(pillar, b.team, 0);
                        break;
                    }
                }
            }
        }

        if(b.timer(2, ringEffectInterval)){
            for(int i = 0; i < crackEffects; i++){
                Tmp.v1.setToRandomDirection().setLength(radius * Mathf.sqrt(Mathf.random())).add(b);
                Tmp.v2.setToRandomDirection().setLength(radius * Mathf.sqrt(Mathf.random())).add(b);
                crackEffect.at(Tmp.v1.x, Tmp.v1.y, Tmp.v1.angleTo(Tmp.v2), crackColor, new LightningData(new Vec2(Tmp.v2), crackStroke));
            }

            if(b.time <= (b.lifetime - ringEffect.lifetime - fadeTime)){
                ringEffect.at(b.x, b.y, radius);
            }
        }
    }

    @Override
    public void draw(Bullet b){
        Lines.stroke(ringStroke, ringColor);
        Draw.alpha(1f - Mathf.curve(b.time, b.lifetime - fadeTime, b.lifetime));
        Lines.circle(b.x, b.y, radius);
    }

    public static class PillarFieldData{
        public Seq<Tile> tiles = new Seq<>();
    }
}