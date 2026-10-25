export function homeLayout(width:number,height:number,insetTop:number){
 const landscape=width>height*1.2,margin=28;
 const cardWidth=landscape?width*.5-margin*2:width-margin*2;
 const fileY=landscape?insetTop+68:Math.max(insetTop+88,height*.18);
 const formatX=landscape?width*.5+14:margin,formatWidth=landscape?width*.5-42:cardWidth;
 const formatY=landscape?insetTop+90:Math.max(fileY+168,height*.42);
 const emptyX=landscape?formatX:margin,emptyHeight=landscape?Math.min(cardWidth,height-100):Math.min(cardWidth,height*.44),emptyY=(height-emptyHeight)/2-12;
 const successX=landscape?formatX:margin,successY=landscape?insetTop+72:Math.max(fileY+120,height*.40);
 return{landscape,margin,cardWidth,fileY,formatX,formatY,formatWidth,tileW:(formatWidth-18)/3,emptyX,emptyY,emptyHeight,successX,successY};
}
