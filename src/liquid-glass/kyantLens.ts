/** Copyright 2025 Kyant. Apache-2.0; licenses/AndroidLiquidGlass-LICENSE.txt.
 * Modified 2026-10-25: circular specialization of AndroidLiquidGlass Shaders.kt
 * at 65ab177e90e5c1d8c62e70cf7755841982da65f6. Global Skia coordinates;
 * safe center normalization; no Compose dependency; seven-sample dispersion retained.
 */
import {Skia} from '@shopify/react-native-skia';
export const KYANT_LENS_SKSL=`
uniform shader content;
uniform float2 c;
uniform float r;
uniform float height;
uniform float amount;
uniform float dispersion;
uniform float2 slosh;
float circleMap(float x) { return 1.0-sqrt(max(0.0,1.0-x*x)); }
half4 main(float2 coord) {
 float2 centered=coord-c;
 float len=length(centered);
 float sd=min(len-r,0.0);
 if(-sd>=height) return content.eval(coord);
 float d=circleMap(clamp(1.0+sd/height,0.0,1.0))*-amount;
 float2 grad=centered/max(len,0.0001);
 float2 refracted=coord+d*grad+slosh*(1.0+sd/max(r,0.001));
 float intensity=dispersion*(centered.x*centered.y)/(r*r);
 float2 spread=d*grad*intensity;
 half4 red=content.eval(refracted+spread);
 half4 orange=content.eval(refracted+spread*(2.0/3.0));
 half4 yellow=content.eval(refracted+spread*(1.0/3.0));
 half4 green=content.eval(refracted);
 half4 cyan=content.eval(refracted-spread*(1.0/3.0));
 half4 blue=content.eval(refracted-spread*(2.0/3.0));
 half4 purple=content.eval(refracted-spread);
 return half4((red.r+orange.r+yellow.r)/3.5+purple.r/7.0,
 orange.g/7.0+(yellow.g+green.g+cyan.g)/3.5,
 (cyan.b+blue.b+purple.b)/3.0,
 (red.a+orange.a+yellow.a+green.a+cyan.a+blue.a+purple.a)/7.0);
}`;
export const kyantLensEffect=Skia.RuntimeEffect.Make(KYANT_LENS_SKSL);
if(!kyantLensEffect)throw new Error('Kyant circular lens failed to compile');
