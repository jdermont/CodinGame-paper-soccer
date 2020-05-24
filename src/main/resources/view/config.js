import { GraphicEntityModule } from './entity-module/GraphicEntityModule.js';

export const demo = {
  playerCount: 2,
  overlayAlpha: 0.4,
  agents: [{
    index: 0,
    name: 'Alice',
    avatar: 'https://www.codingame.com/servlet/fileservlet?id=' + 16085713250612 + '&format=viewer_avatar',
    type: 'CODINGAMER',
    color: '#ff6633',
    typeData: {
      me: true,
      nickname: '[CG]Nonofr'
    }
  }, {
    index: 1,
    name: 'Bob',
    avatar: 'https://www.codingame.com/servlet/fileservlet?id=' + 16085756802960 + '&format=viewer_avatar',
    type: 'CODINGAMER',
    color: '#3366ff',
    typeData: {
      me: true,
      nickname: '[CG]Maxime'
    }
  }],
  frames: [
      
  ]
};

export const playerColors = [
  '#ff6633', // yellow
  '#3366ff' // curious blue
];

export const modules = [
	GraphicEntityModule
];
