#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform vec2 LensCenter;
uniform vec2 ScreenCenter;
uniform vec2 Scale;
uniform vec2 ScaleIn;
uniform vec4 HmdWarpParam;

uniform samplerExternalOES sTexture;
varying vec2 vTextureCoord;

vec2 HmdWarp(vec2 in01)
{
    vec2  theta = (in01 - LensCenter) * ScaleIn; // Scales to [-1, 1]
    float rSq = theta.x * theta.x + theta.y * theta.y;
    vec2  theta1 = theta * (HmdWarpParam.x + HmdWarpParam.y * rSq +
                            HmdWarpParam.z * rSq * rSq + HmdWarpParam.w * rSq * rSq * rSq);
    return LensCenter + Scale * theta1;
}
 
void main()
{
    vec2 tc = HmdWarp(vTextureCoord);

    if (!all(equal(clamp(tc, ScreenCenter-vec2(0.25,0.5), ScreenCenter+vec2(0.25,0.5)), tc)))
        gl_FragColor = vec4(0);
    else
        gl_FragColor = texture2D(sTexture, tc);
}